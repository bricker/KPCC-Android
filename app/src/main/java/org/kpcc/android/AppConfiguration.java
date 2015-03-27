package org.kpcc.android;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfiguration {
    public static final String TAG = "AppConfiguration";
    public static AppConfiguration instance = null;

    private Properties props;

    // If keys.properties is missing or can't be read, you'll get a big ol' IOException.
    protected AppConfiguration(Context context) {
        props = new Properties();

        try {
            AssetManager am = context.getAssets();
            InputStream is = am.open("keys.properties");
            props.load(is);
            is.close();
        } catch (IOException e) {
            // Nothing we can really do.
        }
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new AppConfiguration(context);
        }
    }

    public String getConfig(String config) {
        return props.getProperty(config);
    }
}
