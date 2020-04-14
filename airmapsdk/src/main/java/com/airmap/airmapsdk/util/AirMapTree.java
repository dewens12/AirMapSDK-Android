package com.airmap.airmapsdk.util;

import androidx.annotation.NonNull;

import timber.log.Timber;

/**
 * Mapbox spams Timber with their map tile http requests. This tree ignores all logs with HTTPRequest as their tag
 */
public class AirMapTree extends Timber.DebugTree {

    @Override
    protected boolean isLoggable(String tag, int priority) {
        return !tag.equals("HTTPRequest");
    }

    @Override
    protected void log(int priority, String tag, @NonNull String message, Throwable t) {
        super.log(priority, "AMLog/" + tag, message, t);
    }
}
