package org.kpcc.android;

import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    public Analytics analytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppConfiguration.setupInstance(getApplicationContext());
        Analytics.setupInstance(getApplicationContext());
        analytics = Analytics.getInstance();

        Navigation.getInstance().addItem(0, R.string.kpcc_live,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform() {
                        FragmentManager fragmentManager = getFragmentManager();

                        fragmentManager.beginTransaction()
                                .replace(R.id.container, LiveFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(1, R.string.programs,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform() {
                        FragmentManager fragmentManager = getFragmentManager();

                        fragmentManager.beginTransaction()
                                .replace(R.id.container, ProgramsFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(2, R.string.headlines,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform() {
                        FragmentManager fragmentManager = getFragmentManager();

                        fragmentManager.beginTransaction()
                                .replace(R.id.container, HeadlinesFragment.newInstance())
                                .commit();
                    }
                }
        );

        Navigation.getInstance().addItem(3, R.string.donate,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform() {
                        Uri uri = Uri.parse("http://scpr.org");
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
//                        WebView webView = new WebView(getApplicationContext());
//                        setContentView(webView);
//                        webView.loadUrl("http://www.scpr.org");
                    }
                }
        );

        Navigation.getInstance().addItem(4, R.string.feedback,
                new Navigation.NavigationItemSelectedCallback() {
                    public void perform() {
                        FragmentManager fragmentManager = getFragmentManager();

                        fragmentManager.beginTransaction()
                                .replace(R.id.container, FeedbackFragment.newInstance())
                                .commit();
                    }
                }
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        Navigation.NavigationItem item = Navigation.getInstance().getItem(position);

        // update the main content by replacing fragments
        item.performCallback();
    }

    public void onSectionAttached(int titleId) {
        mTitle = getString(titleId);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        analytics.flush();
        super.onDestroy();
    }
}
