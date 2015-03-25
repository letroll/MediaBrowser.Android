package com.mb.android;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.util.Log;

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
import mediabrowser.apiinteraction.android.sync.PeriodicSync;
import mediabrowser.apiinteraction.playback.PlaybackManager;
import mediabrowser.model.dto.UserDto;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.ClientCapabilities;

import java.io.IOException;
import java.util.ArrayList;


public class MainApplication extends Application
        implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, OnDolbyAudioProcessingEventListener {

    private static final String TAG = "MB3Application";
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
    private DolbyAudioProcessing mDolbyAudioProcessing = null;
    private boolean isDolbyAudioProcessingConnected = false;

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
        capabilities.setDeviceProfile(new AndroidProfile(true, false));
        capabilities.setSupportsMediaControl(true);

        return capabilities;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        _mb3Application = this;
        AppLogger.getLogger().Info("Application object initialized");
        Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

        this.PlayerQueue = new Playlist();
        this.mDevice = new AndroidDevice(this);
        this.mPlaybackManager = new PlaybackManager(mDevice, AppLogger.getLogger());

        Utils.saveFloatToPreference(getApplicationContext(),
                VideoCastManager.PREFS_KEY_VOLUME_INCREMENT, (float) VOLUME_INCREMENT);
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

    // The Dolby Audio Processing instance is created or not
    public boolean isDolbyAvailable() { return (mDolbyAudioProcessing != null); }

    public boolean createDolbyAudioProcessing() {

        try {
            mDolbyAudioProcessing = DolbyAudioProcessing.getDolbyAudioProcessing(this, DolbyAudioProcessing.PROFILE.MOVIE, this);
        } catch (IllegalStateException ex) {
            handleIllegalStateException(ex);
        } catch (IllegalArgumentException ex) {
            handleIllegalArgumentException(ex);
        } catch (RuntimeException ex) {
            handleRuntimeException(ex);
        }

        if (mDolbyAudioProcessing == null) {

            return false;
        }

        isDolbyAudioProcessingConnected = false;

        return true;
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
                AppLogger.getLogger().Info("Dolby Enabled");

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

    @Override
    public void onDolbyAudioProcessingClientConnected() {
        mDolbyAudioProcessing.setEnabled(true);
        AppLogger.getLogger().Debug("DOLBY AUDIO PROCESSING", "DAP is enabled!");
    }

    @Override
    public void onDolbyAudioProcessingClientDisconnected() {
        mDolbyAudioProcessing.setEnabled(false);
    }

    @Override
    public void onDolbyAudioProcessingEnabled(boolean b) {

    }

    @Override
    public void onDolbyAudioProcessingProfileSelected(DolbyAudioProcessing.PROFILE profile) {

    }

    /** Generic handler for IllegalStateException */
    private void handleIllegalStateException(Exception ex)
    {
        AppLogger.getLogger().Error("Dolby Audio Processing has a wrong state");
        handleGenericException(ex);
    }

    /** Generic handler for IllegalArgumentException */
    private void handleIllegalArgumentException(Exception ex)
    {
        AppLogger.getLogger().Error("One of the passed arguments is invalid");
        handleGenericException(ex);
    }

    /** Generic handler for RuntimeException */
    private void handleRuntimeException(Exception ex)
    {
        AppLogger.getLogger().Error("Internal error occured in Dolby Audio Processing");
        handleGenericException(ex);
    }

    /** Logs out the stack trace associated with the Exception*/
    private void handleGenericException(Exception ex)
    {
        AppLogger.getLogger().Error(Log.getStackTraceString(ex));
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
}
