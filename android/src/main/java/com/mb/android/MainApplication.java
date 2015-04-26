package com.mb.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.multidex.MultiDex;

import com.dolby.dap.DolbyAudioProcessing;
import com.dolby.dap.OnDolbyAudioProcessingEventListener;
import com.mb.android.displaypreferences.DisplayPreferenceManager;
import com.mb.android.exceptions.DefaultExceptionHandler;
import com.mb.android.interfaces.IWebsocketEventListener;
import com.mb.android.playbackmediator.cast.VideoCastManager;
import com.mb.android.playbackmediator.utils.Utils;
import com.mb.android.activities.mobile.RemoteControlActivity;
import com.mb.android.player.AudioService;
import com.mb.android.logging.AppLogger;
import mediabrowser.apiinteraction.IConnectionManager;
import mediabrowser.apiinteraction.android.AndroidApiClient;
import mediabrowser.apiinteraction.android.AndroidConnectionManager;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.profiles.AndroidProfile;
import mediabrowser.apiinteraction.android.profiles.AndroidProfileOptions;
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.android.sync.data.AndroidAssetManager;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.apiinteraction.sync.data.ILocalAssetManager;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dto.ImageOptions;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.entities.ImageType;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

import java.io.IOException;
import java.util.ArrayList;


public class MainApplication extends Application
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, OnDolbyAudioProcessingEventListener {

    // This constant is to show that depending on your application's logic
    // You might choose to handle pause/resume Dolby audio processing session
    // When the application goes background/foreground
    private static final boolean RELEASE_DOLBY_CONTROL_WHEN_IN_BACKGROUND = true;

    public static final double VOLUME_INCREMENT = 0.05;
    private static MainApplication _mb3Application;
    // v1 Id AE4DA10A
    // v2 Id 472F0435
    // v3 Id 69C59853
    // v4 Id F4EB2E8E
    // default receiver chrome.cast.media.DEFAULT_MEDIA_RECEIVER_APP_ID
    private static final String APPLICATION_ID = "F4EB2E8E";
    private static final String DATA_NAMESPACE = "urn:x-cast:com.google.cast.mediabrowser.v3";
    private static VideoCastManager mCastMgr = null;
    private static AudioService mAudioService = null;
    public AndroidApiClient API;
    public UserDto user;
    public Playlist PlayerQueue;
//    public String LibretroNativeLibraryPath;
    private MediaPlayer mMediaPlayer;
    private PlaybackManager mPlaybackManager;
    private AndroidDevice mDevice;

    private Object  mLock = null;
    // The Activity list, the list will be empty when the application goes background
    private final java.util.List<String> mActList = new java.util.ArrayList<String>();

    // Handle to Dolby Audio Processing
    private DolbyAudioProcessing mDolbyAudioProcessing = null;

    // Internal flag to maintain the connection status
    private boolean isDolbyAudioProcessingConnected = false;

    private IDolbyActivity mDolbyActivity;

    private boolean isOffline = false; //future

    public static MainApplication getInstance() {
        return _mb3Application;
    }

    public static String getApplicationId() {
        return APPLICATION_ID;
    }
    public static VideoCastManager getCastManager(Context context) {
        if (mCastMgr == null) {
            mCastMgr = VideoCastManager.initialize(context, APPLICATION_ID,
                    RemoteControlActivity.class, DATA_NAMESPACE);
            mCastMgr.enableFeatures(
                    VideoCastManager.FEATURE_NOTIFICATION |
                            VideoCastManager.FEATURE_LOCKSCREEN |
                            VideoCastManager.FEATURE_DEBUGGING
            );
        }
        mCastMgr.setContext(context);
        mCastMgr.setStopOnDisconnect(true);
        return mCastMgr;
    }

    public static AudioService getAudioService() {
        if (mAudioService == null) {
            mAudioService = AudioService.initialize();
        }
        return mAudioService;
    }

    private IJsonSerializer jsonSerializer;

    public IJsonSerializer getJsonSerializer() {
        if (jsonSerializer == null) {
            jsonSerializer = new GsonJsonSerializer();
        }
        return jsonSerializer;
    }
    private IConnectionManager connectionManager;

    public IConnectionManager getConnectionManager() {
        if (connectionManager == null) {

            connectionManager = new AndroidConnectionManager(
                    this,
                    getJsonSerializer(),
                    AppLogger.getLogger(),
                    new VolleyHttpClient(AppLogger.getLogger(), this),
                    "Android",
                    getApplicationVersion(),
                    getClientCapabilities(),
                    new MbApiEventListener()
            );
        }

        return connectionManager;
    }

    private ClientCapabilities getClientCapabilities() {
        ClientCapabilities capabilities = new ClientCapabilities();

        ArrayList<String> playableTypes = new ArrayList<>();
        playableTypes.add("Audio");
        playableTypes.add("Video");

        ArrayList<String> supportedCommands = new ArrayList<>();
        supportedCommands.add("MoveUp");
        supportedCommands.add("MoveDown");
        supportedCommands.add("MoveLeft");
        supportedCommands.add("MoveRight");
        supportedCommands.add("PageUp");
        supportedCommands.add("PageDown");
        supportedCommands.add("Select");
        supportedCommands.add("Back");
        supportedCommands.add("TakeScreenshot");
        supportedCommands.add("GoHome");
        supportedCommands.add("GoToSettings");
        supportedCommands.add("VolumeUp");
        supportedCommands.add("VolumeDown");
        supportedCommands.add("ToggleMute");
        supportedCommands.add("DisplayContent");

        capabilities.setPlayableMediaTypes(playableTypes);
        capabilities.setSupportedCommands(supportedCommands);
        capabilities.setSupportsContentUploading(true);
        capabilities.setSupportsSync(true);
        capabilities.setSupportsOfflineAccess(true);
        capabilities.setDeviceProfile(getDeviceProfile());
        capabilities.setSupportsMediaControl(true);

        capabilities.setIconUrl("https://raw.githubusercontent.com/MediaBrowser/MediaBrowser.Android/master/servericon.png");

        return capabilities;
    }

    private boolean isDolbySupported = false;

    public DeviceProfile getDeviceProfile() {

        AndroidProfileOptions options = new AndroidProfileOptions();
        options.DefaultH264Level = 41;
        options.SupportsHls = true;
        options.SupportsAc3 = isDolbySupported;

        return new AndroidProfile(options);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _mb3Application = this;
        AppLogger.getLogger().Info("Application object initialized");
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

        isDolbySupported = createDolbyAudioProcessing();

        this.PlayerQueue = new Playlist();
        this.mDevice = new AndroidDevice(this);
        ILocalAssetManager localAssetManager = new AndroidAssetManager(this, AppLogger.getLogger(), getJsonSerializer());
        this.mPlaybackManager = new PlaybackManager(localAssetManager, mDevice, AppLogger.getLogger());

        Utils.saveFloatToPreference(getApplicationContext(), VideoCastManager.PREFS_KEY_VOLUME_INCREMENT, (float) VOLUME_INCREMENT);

        // When the application is created, it registers to listen for all it's activities' lifecycle
        registerAppCallbacks();
    }

    public void PlayMedia(String url) {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        try {
            mMediaPlayer.setDataSource(url);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
        mMediaPlayer.setVolume(.2f, .2f);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public void StopMedia() {

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                AppLogger.getLogger().Info("SeriesViewActivity: mMediaPlayer.stop called");
            }
        } catch (IllegalStateException e) {
            AppLogger.getLogger().Info("SeriesViewActivity: mMediaPlayer IllegalStateException");
        } finally {
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                AppLogger.getLogger().Info("SeriesViewActivity: mMediaPlayer.release called");
                mMediaPlayer = null;
            }
        }
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {

        try {
            mediaPlayer.release();
        } catch (Exception e) {
            AppLogger.getLogger().Info("SeriesViewActivity: Error releasing MediaPlayer");
        }

    }

    //**********************************************************************************************
    // Dolby Methods
    //**********************************************************************************************

    // The ActivityLifecycleCallbacks for the application Activities
    private AppActivityLifecycleCallbacks mAppCallback = new AppActivityLifecycleCallbacks();

    private class AppActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks
    {

        @Override
        public void onActivityCreated(Activity activity,
                                      Bundle savedInstanceState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
            synchronized (mLock) {
                String name = activity.getClass().getName();
                AppLogger.getLogger().Info("onActivityResumed: " + name);
                if(!mActList.contains(name)){
                    mActList.add(name);
                    AppLogger.getLogger().Info("Activitys:" + mActList.toString());
                }

                //
                // If audio playback is not required while your application is in the background, restore the Dolby audio processing system
                // configuration to its original state by suspendSession().
                // This ensures that the use of the system-wide audio processing is sandboxed to your application.

                if (RELEASE_DOLBY_CONTROL_WHEN_IN_BACKGROUND) {
                    restartSession();
                }
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity,
                                                Bundle outState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            // The developer can detect if the application goes background here
            // and broadcast some intents to notify that the application is going background and call suspendSession().
            synchronized (mLock) {
                String name = activity.getClass().getName();
                AppLogger.getLogger().Info("onActivityStopped: " + name);
                if(mActList.contains(name)){
                    mActList.remove(name);
                    AppLogger.getLogger().Info("Activitys:" + mActList.toString());
                }

                if (RELEASE_DOLBY_CONTROL_WHEN_IN_BACKGROUND) {
                    if (isAppInBackground()) {
                        AppLogger.getLogger().Info("The application is in background, supsendSession");
                        //
                        // If audio playback is not required while your application is in the background, restore the Dolby audio processing system
                        // configuration to its original state by suspendSession().
                        // This ensures that the use of the system-wide audio processing is sandboxed to your application.
                        suspendSession();
                    }
                }
            }
        }

    }

    public void registerAppCallbacks() {

        mLock = this;
        // Register the Application.registerActivityLifecycleCallbacks to help detect the application's background
        synchronized (mLock) {
            this.registerActivityLifecycleCallbacks(mAppCallback);
            AppLogger.getLogger().Info("registerActivityLifecycleCallbacks is done.");
        }
    }

    // Unregister the Application ActivityLifecycleCallbacks
    public void unregisterAppCallbacks() {
        synchronized (mLock) {
            this.unregisterActivityLifecycleCallbacks(mAppCallback);
            AppLogger.getLogger().Info("unregisterActivityLifecycleCallbacks is done.");
        }
    }

    // If there is an Activity is showing in foreground
    public boolean isAppInBackground() {
        boolean empty = mActList.isEmpty();
        AppLogger.getLogger().Info("isAppInBackground:" + empty + " Activitys:"+mActList.toString());
        return empty;
    }

    // Set the UI Activity instance
    public void setDolbyActivity(IDolbyActivity act) {
        mDolbyActivity = act;
    }

    // The Dolby Audio Processing instance is created or not
    public boolean isDolbyAvailable() {
        return (mDolbyAudioProcessing != null);
    }

    // Create the Dolby Audio Processing instance
    public boolean createDolbyAudioProcessing() {

        // Obtain the handle to Dolby Audio Processing
        // DolbyAudioProcessing objects shall not be used until onClientConnected() is called.
        // Use the Movie profile on Kindle Fire
        try{
            mDolbyAudioProcessing = DolbyAudioProcessing.getDolbyAudioProcessing(this, DolbyAudioProcessing.PROFILE.MOVIE, this);
        } catch (IllegalStateException ex) {
            handleIllegalStateException(ex);
        } catch (IllegalArgumentException ex) {
            handleIllegalArgumentException(ex);
        } catch (RuntimeException ex) {
            handleRuntimeException(ex);
        }


        // Not all Android devices have Dolby Audio Processing integrated. So DolbyAudioProcessing may not be available.
        if (mDolbyAudioProcessing == null) {

            AppLogger.getLogger().Info("Dolby Audio Processing can't be instantiated on this device.");

            return false;
        }

        isDolbyAudioProcessingConnected = false;

        return true;
    }

    // Backup the system-wide audio effect configuration and restore the application configuration
    public void restartSession() {
        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try{
                mDolbyAudioProcessing.restartSession();
            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
    }

    // Backup the application Dolb Audio Processing configuration and restore the system-wide configuration
    public void suspendSession() {

        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try{
                mDolbyAudioProcessing.suspendSession();
            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
    }

    // Release the instance of Dolby Audio Processing
    public void releaseDolbyAudioProcessing() {
        if (mDolbyAudioProcessing != null) {
            try {
                mDolbyAudioProcessing.release();
                mDolbyAudioProcessing = null;
            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }

        isDolbyAudioProcessingConnected = false;
    }

    // Is the Dolby Audio Processing enabled
    public boolean isDolbyAudioProcessingEnabled() {
        boolean bRet = false;
        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try{
                bRet = mDolbyAudioProcessing.isEnabled();
            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
        return bRet;
    }

    // Get the current selected profile of Dolby Audio Processing
    public DolbyAudioProcessing.PROFILE getCurrentSelectedProfile() {
        DolbyAudioProcessing.PROFILE profile = DolbyAudioProcessing.PROFILE.MOVIE;
        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try {
                profile = mDolbyAudioProcessing.getSelectedProfile();
            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
        return profile;
    }

    // Enable/disable the Dolby Audio Processing
    public void setDolbyAudioProcessingEnabled(boolean enable) {
        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try {

                // Enable/disable Dolby Audio Processing
                mDolbyAudioProcessing.setEnabled(enable);

            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
    }

    // Set the active Dolby Audio Processing profile
    public void setDolbyAudioProcessingProfile(String profile) {
        if (mDolbyAudioProcessing != null && isDolbyAudioProcessingConnected) {
            try {

                // Set Dolby Audio Processing profile
                mDolbyAudioProcessing.setProfile(DolbyAudioProcessing.PROFILE.valueOf(profile));

            } catch (IllegalStateException ex) {
                handleIllegalStateException(ex);
            } catch (IllegalArgumentException ex) {
                handleIllegalArgumentException(ex);
            } catch (RuntimeException ex) {
                handleRuntimeException(ex);
            }
        }
    }

    /** Generic handler for IllegalStateException */
    private void handleIllegalStateException(Exception ex)
    {
        handleGenericException(ex);
    }

    /** Generic handler for IllegalArgumentException */
    private void handleIllegalArgumentException(Exception ex)
    {
        handleGenericException(ex);
    }

    /** Generic handler for RuntimeException */
    private void handleRuntimeException(Exception ex)
    {
        handleGenericException(ex);
    }

    /** Logs out the stack trace associated with the Exception*/
    private void handleGenericException(Exception ex)
    {
        AppLogger.getLogger().ErrorException("Error in Dolby Processing", ex);
    }

    /******************************************************************************
     * Following methods provide an implementation of the listener interface
     * {@link com.dolby.ds.OnDolbyAudioProcessingEventListener}
     ******************************************************************************/
    @Override
    public void onDolbyAudioProcessingClientConnected() {
        // Dolby Audio Processing has connected we can now initialise the UI elements
        isDolbyAudioProcessingConnected = true;

        if (mDolbyActivity != null) {
            mDolbyActivity.clientConnected();
        }
    }

    @Override
    public void onDolbyAudioProcessingClientDisconnected() {
        // Application's Dolby Audio Processing handle has been abnormally disconnected from the system service
        isDolbyAudioProcessingConnected = false;

        if (mDolbyActivity != null) {
            mDolbyActivity.clientDisconnected();
        }
    }

    @Override
    public void onDolbyAudioProcessingEnabled(boolean on) {
        // Called when the system Dolby audio processing has been set
        // by another running application.
        // Intended to be used for notification purposes only, the application should
        // choose an appropriate action based on the use-case.
        // The foreground application has the full control of Dolby audio processing and the external system-wide changes will be overridden.
        // When the application is in the background, this callback is invoked to notify the application of a state setting event.
        //
        // Note: To avoid a system state race condition, this callback should never
        // be used to reset the Dolby audio processing enabled state.
        AppLogger.getLogger().Info("onDolbyAudioProcessingEnabled is received : "+on);

    }

    @Override
    public void onDolbyAudioProcessingProfileSelected(DolbyAudioProcessing.PROFILE profile) {
        // Called when the system Dolby audio processing profile has been selected
        // by another running application.
        // Intended to be used for notification purposes only, the application should
        // choose an appropriate action based on the use-case.
        // The foreground application has the full control of Dolby audio processing and the external system-wide changes will be overridden.
        // When the application is in the background, this callback is invoked to notify the application of a profile selection event.
        //
        // Note: To avoid a system state race condition, this callback should never
        // be used to reset the Dolby audio processing profile selection.
        AppLogger.getLogger().Info("onDolbyAudioProcessingProfileSelected is received : "+profile);

    }

    public String getApplicationVersion() {
        String appVersion = "1.0.0";
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            appVersion = pInfo.versionName;
        }

        return appVersion;
    }

    private IWebsocketEventListener mCurrentActivity = null;

    public IWebsocketEventListener getCurrentActivity(){
        return mCurrentActivity;
    }
    public void setCurrentActivity(IWebsocketEventListener mCurrentActivity){
        this.mCurrentActivity = mCurrentActivity;
    }

    private DisplayPreferenceManager preferenceManager;
    public DisplayPreferenceManager getPreferenceManager() {
        if (preferenceManager == null) {
            preferenceManager = new DisplayPreferenceManager();
        }
        return preferenceManager;
    }

    public void startContentSync() {
        new PeriodicSync(_mb3Application).Create();
    }

    public PlaybackManager getPlaybackManager() {
        return mPlaybackManager;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean isOffline) {
        this.isOffline = isOffline;
    }

    public AndroidDevice getDevice() {
        return mDevice;
    }

    public ImageOptions getImageOptions(ImageType type){

        ImageOptions options = new ImageOptions();
        options.setImageType(type);

        if (type == ImageType.Backdrop){
            options.setQuality(60);
        }
        else{
            options.setQuality(80);
        }

        return options;
    }
}
