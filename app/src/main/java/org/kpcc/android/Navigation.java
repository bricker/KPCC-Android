package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.FragmentManager;

import java.util.ArrayList;

class Navigation {
    private static final Navigation instance = new Navigation();

    // We're declaring the size statically so we can use native Array.
    final ArrayList<NavigationItem> navigationItems = new ArrayList<>();

    static Navigation getInstance() {
        return instance;
    }

    void addItem(int titleId, int iconId, String stackTag,
                        String analyticsKey, NavigationItemSelectedCallback callback) {
        navigationItems.add(new NavigationItem(titleId, iconId, stackTag, analyticsKey, callback));
    }

    static abstract class NavigationItemSelectedCallback {
        public abstract void perform(FragmentManager fm, boolean addToBackStack);
    }

    static class NavigationItem {
        private final int mTitleId;
        private final int mIconId;
        private final String mStackTag;
        private final NavigationItemSelectedCallback mCallback;

        public static Integer getIndexByStackTag(String stackTag) {
            Integer found = null;

            for (int i=0; i < Navigation.getInstance().navigationItems.size(); i++) {
                NavigationItem navigationItem = Navigation.getInstance().navigationItems.get(i);

                if (navigationItem.getStackTag().equals(stackTag)) {
                    found = i;
                    break;
                }
            }

            return found;
        }

        NavigationItem(int titleId, int iconId, String stackTag,
                       String analyticsKey, NavigationItemSelectedCallback callback) {
            this.mTitleId = titleId;
            this.mIconId = iconId;
            this.mCallback = callback;
            this.mStackTag = stackTag;
        }

        void performCallback(Context context, boolean addToBackStack) {
            MainActivity activity = ((MainActivity) context);
            FragmentManager fm = activity.getSupportFragmentManager();

            mCallback.perform(fm, addToBackStack);
        }

        int getTitleId() {
            return mTitleId;
        }

        int getIconId() {
            return mIconId;
        }

        String getStackTag() {
            return mStackTag;
        }
    }
}
