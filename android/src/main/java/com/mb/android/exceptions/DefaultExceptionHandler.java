package com.mb.android.exceptions;

import com.mb.android.logging.AppLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by Mark on 2014-09-17.
 */
public class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {

    public void uncaughtException(Thread thread, Throwable ex) {
        AppLogger.getLogger().Error("Uncaught Exception ");
        LogStackTrace(ex);
    }

    public void LogStackTrace(Throwable ex) {

        Writer result = new StringWriter();
        PrintWriter printWriter = new PrintWriter(result);
        ex.printStackTrace(printWriter);
        AppLogger.getLogger().Error("Stack Trace: " + result.toString());
        if (ex.getCause() != null) {
            LogStackTrace(ex.getCause());
        }

        printWriter = null;
        result = null;
    }
}

