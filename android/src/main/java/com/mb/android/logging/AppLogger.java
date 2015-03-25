package com.mb.android.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.mb.android.MainApplication;
import com.mb.android.utils.Utils;

import ch.qos.logback.classic.Level;
import mediabrowser.apiinteraction.android.sync.MediaSyncAdapter;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.logging.LogSeverity;

import java.io.File;
import java.util.UUID;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;


public class AppLogger {

    private static LogbackLogger mInstance;

    public static ILogger getLogger() {

        if (mInstance == null) {
            org.slf4j.Logger internalLogger = configureLogbackDirectly();

            mInstance = new LogbackLogger(internalLogger, "App");
            MediaSyncAdapter.LoggerFactory = new SyncLoggerFactory(new LogbackLogger(internalLogger, "SyncService"));
            WriteLogHeader(mInstance);
        }

        return mInstance;
    }

    private static FileAppender<ILoggingEvent> syncServiceFileAppender;

    private static org.slf4j.Logger configureLogbackDirectly() {

        // reset the default context (which may already have been initialized)
        // since we want to reconfigure it
        LoggerContext lc = (LoggerContext)LoggerFactory.getILoggerFactory();
        lc.reset();

        // setup FileAppender
        PatternLayoutEncoder encoder1 = new PatternLayoutEncoder();
        encoder1.setContext(lc);
        encoder1.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
        encoder1.start();

        FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
        fileAppender.setContext(lc);
        fileAppender.setEncoder(encoder1);
        fileAppender.setName("fileAppender");
        fileAppender.setFile(getLogFilePath(""));
        fileAppender.addFilter(new LogFileFilter(false));
        fileAppender.start();

        syncServiceFileAppender = new FileAppender<ILoggingEvent>();
        syncServiceFileAppender.setContext(lc);
        syncServiceFileAppender.setEncoder(encoder1);
        syncServiceFileAppender.setName("syncServiceFileAppender");
        syncServiceFileAppender.addFilter(new LogFileFilter(true));

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(lc);
        logcatAppender.setEncoder(encoder1);
        logcatAppender.setName("logcatAppender");
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
        root.addAppender(logcatAppender);
        root.addAppender(syncServiceFileAppender);

        return LoggerFactory.getLogger("App");
    }

    public static void ResetSyncLogger(){
        syncServiceFileAppender.stop();
        syncServiceFileAppender.setFile(getLogFilePath("syncService-"));
        syncServiceFileAppender.start();
    }

    private static String getLogFilePath(String prefix){

        String filename = prefix + UUID.randomUUID().toString() + ".log";

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }

        if (mExternalStorageAvailable && mExternalStorageWriteable){
            File directory = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "emby");
            directory = new File(directory, "logs");
            return new File(directory, filename).getPath();
        }
        else{
            return MainApplication.getInstance().getFileStreamPath(filename).getAbsolutePath();
        }
    }

    public static void setDebugLoggingEnabled(boolean enabled){

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (enabled){
            root.setLevel(Level.DEBUG);
        }
        else{
            root.setLevel(Level.INFO);
        }
    }

    private static void WriteLogHeader(ILogger logger) {

        PackageInfo pInfo = null;
        try {
            pInfo = MainApplication.getInstance().getPackageManager().getPackageInfo(MainApplication.getInstance().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            logger.Info("Application Version: " + pInfo.versionName);
        }

        if (Build.VERSION.RELEASE != null) {
            logger.Info("Android Version: " + Build.VERSION.RELEASE);
        }

        if (Build.MODEL != null && !Build.MODEL.isEmpty())
            logger.Info("Device: " + Build.MODEL);

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) MainApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getMetrics(metrics);
            logger.Info("Screen Width: " + String.valueOf(metrics.widthPixels));
            logger.Info("Screen Height: " + String.valueOf(metrics.heightPixels));
            logger.Info("Density: " + String.valueOf(metrics.density));
            logger.Info("DensityDpi: " + String.valueOf(metrics.densityDpi));

        } catch (Exception e) {

        }

        logger.Info("Total Memory Available: " + Utils.TotalMemory(MainApplication.getInstance()));
        logger.Info("Max Memory usable per Application: " + Utils.MaxApplicationMemory());
    }
}
