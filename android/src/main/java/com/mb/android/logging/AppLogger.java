package com.mb.android.logging;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

import com.mb.android.MainApplication;
import com.mb.android.utils.Utils;

import ch.qos.logback.classic.Level;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.logging.LogSeverity;
import java.util.UUID;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;


public class AppLogger implements ILogger {

    private static AppLogger mInstance;

    private AppLogger() {

        if (internalLogger == null){
            configureLogbackDirectly();
        }
    }

    public static AppLogger getLogger() {

        if (mInstance == null) {
            mInstance = new AppLogger();
            mInstance.WriteLogHeader();
        }

        return mInstance;
    }

    private org.slf4j.Logger internalLogger;
    private void configureLogbackDirectly() {

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

        String path = UUID.randomUUID().toString() + ".log";

        fileAppender.setFile(MainApplication.getInstance().getFileStreamPath(path).getAbsolutePath());
        fileAppender.setEncoder(encoder1);
        fileAppender.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setEncoder(encoder1);
        logcatAppender.setName("App");
        logcatAppender.setContext(lc);
        logcatAppender.start();

        // add the newly created appenders to the root logger;
        // qualify Logger to disambiguate from org.slf4j.Logger
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.addAppender(fileAppender);
        root.addAppender(logcatAppender);

        internalLogger = LoggerFactory.getLogger("App");
    }

    public void setDebugLoggingEnabled(boolean enabled){

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

        if (enabled){
            root.setLevel(Level.DEBUG);
        }
        else{
            root.setLevel(Level.INFO);
        }
    }

    private void WriteLogHeader() {

        PackageInfo pInfo = null;
        try {
            pInfo = MainApplication.getInstance().getPackageManager().getPackageInfo(MainApplication.getInstance().getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (pInfo != null) {
            Info("Application Version: " + pInfo.versionName);
        }

        if (Build.VERSION.RELEASE != null) {
            Info("Android Version: " + Build.VERSION.RELEASE);
        }

        if (Build.MODEL != null && !Build.MODEL.isEmpty())
            Info("Device: " + Build.MODEL);

        try {
            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm = (WindowManager) MainApplication.getInstance().getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            display.getMetrics(metrics);
            Info("Screen Width: " + String.valueOf(metrics.widthPixels));
            Info("Screen Height: " + String.valueOf(metrics.heightPixels));
            Info("Density: " + String.valueOf(metrics.density));
            Info("DensityDpi: " + String.valueOf(metrics.densityDpi));

        } catch (Exception e) {

        }

        Info("Total Memory Available: " + Utils.TotalMemory(MainApplication.getInstance()));
        Info("Max Memory usable per Application: " + Utils.MaxApplicationMemory());
    }

    @Override
    public void Info(String message, Object... paramList) {
        internalLogger.info(String.format(message, paramList));
    }


    @Override
    public void Error(String message, Object... paramList) {
        internalLogger.error(String.format(message, paramList));
    }


    @Override
    public void Warn(String message, Object... paramList) {
        internalLogger.warn(String.format(message, paramList));
    }


    @Override
    public void Debug(String message, Object... paramList) {
        internalLogger.debug(String.format(message, paramList));
    }


    @Override
    public void Fatal(String message, Object... paramList) {
        internalLogger.error(String.format(message, paramList));
    }


    @Override
    public void FatalException(String message, Exception exception, Object... paramList) {
        logException(String.format(message, paramList), exception, LogSeverity.Fatal);
    }


    @Override
    public void ErrorException(String message, Exception exception, Object... paramList) {
        logException(String.format(message, paramList), exception, LogSeverity.Error);
    }

    //******************************************************************************************************************

    private void logException(String message, Exception exception, LogSeverity severity) {

        try {
            message += "\r" + stackTraceToString(exception);
            if (exception.getCause() != null) {
                message += "caused by " + stackTraceToString(exception.getCause());
            }
        } catch (Exception e) {
            Error("FileLogger", "failed to parse exception");
        }

        internalLogger.error(message);
    }

    private String stackTraceToString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\r");
        }
        return sb.toString();
    }
}
