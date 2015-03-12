package com.mb.android.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * Created by Luke on 3/12/2015.
 */
public class LogFileFilter extends Filter<ILoggingEvent> {

    private boolean isSyncLogger;

    public LogFileFilter(boolean isSyncLogger) {
        this.isSyncLogger = isSyncLogger;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {

        if (event.getMarker().contains("SyncService")){
            return isSyncLogger ? FilterReply.ACCEPT : FilterReply.DENY;
        }

        return isSyncLogger ? FilterReply.DENY : FilterReply.ACCEPT;
    }
}
