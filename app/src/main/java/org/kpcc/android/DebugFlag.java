package org.kpcc.android;

/**
 * Created by rickb014 on 8/8/15.
 */
public class DebugFlag {
    public final static boolean PREROLL = false;

    public static boolean isEnabled(boolean flag) {
        return BuildConfig.DEBUG && flag;
    }
}
