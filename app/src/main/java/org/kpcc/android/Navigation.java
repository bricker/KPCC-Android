package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public class Navigation {
    public static final Navigation instance = new Navigation();

    // We're declaring the size statically so we can use native Array.
    public final NavigationItem[] navigationItems = new NavigationItem[6];

    public void addItem(int idx, int titleId, int iconId, String stackTag,
                        String analyticsKey, NavigationItemSelectedCallback callback) {
        navigationItems[idx] = new NavigationItem(titleId, iconId, stackTag, analyticsKey, callback);
    }

    public static abstract class NavigationItemSelectedCallback {
        public abstract void perform(FragmentManager fm, boolean addToBackStack);
    }

    public static class NavigationItem {
        public final int titleId;
        public final int iconId;
        public final String stackTag;
        private final String analyticsKey;
        private final NavigationItemSelectedCallback callback;

        NavigationItem(int titleId, int iconId, String stackTag,
                       String analyticsKey, NavigationItemSelectedCallback callback) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.analyticsKey = analyticsKey;
            this.callback = callback;
            this.stackTag = stackTag;
        }

        public void performCallback(Context context, boolean logEvent, boolean addToBackStack) {
            MainActivity activity = ((MainActivity) context);
            FragmentManager fm = activity.getSupportFragmentManager();

            if (logEvent) {
                logMenuItemSelection();
            }

            callback.perform(fm, addToBackStack);
        }

        private void logMenuItemSelection() {
            AnalyticsManager.instance.logEvent(analyticsKey);
        }
    }
}
