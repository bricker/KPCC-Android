package org.kpcc.android;

public class Navigation {
    private static Navigation instance = null;

    // We're declaring the size statically so we can use native Array.
    private NavigationItem[] mNavigationItems = new NavigationItem[5];

    protected Navigation() { }

    public static Navigation getInstance() {
        if (instance == null) {
            instance = new Navigation();
        }

        return instance;
    }

    public NavigationItem[] getItems() {
        return mNavigationItems;
    }

    public void addItem(int idx, int titleId, NavigationItemSelectedCallback callback) {
        mNavigationItems[idx] = new NavigationItem(titleId, callback);
    }

    public NavigationItem getItem(int idx) {
        return mNavigationItems[idx];
    }

    public int getSize() {
        return mNavigationItems.length;
    }


    public static class NavigationItem {
        private int mTitleId;
        private NavigationItemSelectedCallback mCallback;

        protected NavigationItem(int titleId, NavigationItemSelectedCallback callback) {
            mTitleId = titleId;
            mCallback = callback;
        }

        public int getTitleId() {
            return mTitleId;
        }

        public void performCallback() {
            mCallback.perform();
        }
    }

    public static interface NavigationItemSelectedCallback {
        public void perform();
    }
}
