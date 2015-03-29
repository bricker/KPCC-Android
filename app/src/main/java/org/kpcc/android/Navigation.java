package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public class Navigation {
    public static final Navigation instance = new Navigation();

    // We're declaring the size statically so we can use native Array.
    public final NavigationItem[] navigationItems = new NavigationItem[6];

    public void addItem(int idx, int titleId, int iconId,
                        String analyticsKey, NavigationItemSelectedCallback callback) {
        navigationItems[idx] = new NavigationItem(titleId, iconId, analyticsKey, callback);
    }

    public static abstract class NavigationItemSelectedCallback {
        public abstract void perform(FragmentManager fm);
    }

    public static class NavigationItem {
        public final int titleId;
        public final int iconId;
        private final String analyticsKey;
        private final NavigationItemSelectedCallback callback;

        NavigationItem(int titleId, int iconId,
                       String analyticsKey, NavigationItemSelectedCallback callback) {
            this.titleId = titleId;
            this.iconId = iconId;
            this.analyticsKey = analyticsKey;
            this.callback = callback;
        }

        public void performCallback(Context context) {
            MainActivity activity = ((MainActivity) context);
            FragmentManager fm = activity.getSupportFragmentManager();
            logMenuItemSelection();
            callback.perform(fm);
        }

        private void logMenuItemSelection() {
            AnalyticsManager.instance.logEvent(analyticsKey);
        }
    }
}
