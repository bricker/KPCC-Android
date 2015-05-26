package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;

public class Navigation {
    public static final Navigation instance = new Navigation();

    // We're declaring the size statically so we can use native Array.
    public final ArrayList<NavigationItem> navigationItems = new ArrayList<>();

    public void addItem(int titleId, int iconId, String stackTag,
                        String analyticsKey, NavigationItemSelectedCallback callback) {
        navigationItems.add(new NavigationItem(titleId, iconId, stackTag, analyticsKey, callback));
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

        public static Integer getIndexByStackTag(String stackTag) {
            Integer found = null;

            for (int i=0; i < Navigation.instance.navigationItems.size(); i++) {
                NavigationItem navigationItem = Navigation.instance.navigationItems.get(i);

                if (navigationItem.stackTag.equals(stackTag)) {
                    found = i;
                    break;
                }
            }

            return found;
        }

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
