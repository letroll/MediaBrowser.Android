package com.mb.android.logging;

import mediabrowser.apiinteraction.android.sync.ISyncLoggerFactory;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 3/12/2015.
 */
public class SyncLoggerFactory implements ISyncLoggerFactory {

    private ILogger syncLogger;

    public SyncLoggerFactory(ILogger syncLogger) {
        this.syncLogger = syncLogger;
    }

    @Override
    public ILogger getNewLogger() {
        AppLogger.ResetSyncLogger();
        return syncLogger;
    }

}
