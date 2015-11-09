package org.kpcc.android;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;


public class AppConfiguration {
    public static AppConfiguration instance = null;
    public final boolean isDebug;
    private final Properties props;
    private final Properties secrets;

    // If keys.properties is missing or can't be read, you'll get a big ol' IOException.
    private AppConfiguration(Context context) {
        props = new Properties();
        secrets = new Properties();

        try {
            AssetManager am = context.getAssets();
            InputStream keys = am.open("keys.properties");
            secrets.load(keys);
            keys.close();

            InputStream config = am.open("config.properties");
            props.load(config);
            config.close();
        } catch (IOException e) {
            // Nothing we can really do.
        }

        isDebug = Boolean.parseBoolean(props.getProperty("debug"));
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new AppConfiguration(context);
        }
    }

    public String getConfig(String config) {
        String mconfig = config;
        if (BuildConfig.DEBUG && isDebug) { mconfig = "debug." + config; }

        String prop = props.getProperty(mconfig);
        if (prop == null) { prop = props.getProperty(config); }

        return prop;
    }

    public String getSecret(String config) {
        return secrets.getProperty(config);
    }
}
