package org.kpcc.android;

import android.content.Context;
import android.support.v4.app.FragmentManager;

public class Navigation {
    private static Navigation instance = null;

    // We're declaring the size statically so we can use native Array.
    private NavigationItem[] mNavigationItems = new NavigationItem[6];

    protected Navigation() {
    }

    public static Navigation getInstance() {
        if (instance == null) {
            instance = new Navigation();
        }

        return instance;
    }

    public NavigationItem[] getItems() {
        return mNavigationItems;
    }

    public void addItem(int idx, int titleId, int iconId, NavigationItemSelectedCallback callback) {
        mNavigationItems[idx] = new NavigationItem(titleId, iconId, callback);
    }

    public NavigationItem getItem(int idx) {
        return mNavigationItems[idx];
    }

    public int getSize() {
        return mNavigationItems.length;
    }


    public static interface NavigationItemSelectedCallback {
        public void perform(FragmentManager fm);
    }

    public static class NavigationItem {
        private int mTitleId;
        private int mIconId;
        private NavigationItemSelectedCallback mCallback;

        protected NavigationItem(int titleId, int iconId, NavigationItemSelectedCallback callback) {
            mTitleId = titleId;
            mIconId = iconId;
            mCallback = callback;
        }

        public int getTitleId() {
            return mTitleId;
        }
        public int getIconId() {
            return mIconId;
        }

        public void performCallback(Context context) {
            MainActivity activity = ((MainActivity) context);
            FragmentManager fm = activity.getSupportFragmentManager();
            mCallback.perform(fm);
        }
    }
}
