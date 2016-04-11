package org.kpcc.android;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public class AppConfiguration {
    private static AppConfiguration instance = null;
    final boolean isDebug;
    private final Properties props;
    private final Properties secrets;
    private final ConcurrentHashMap<String, String> dynamicProperties= new ConcurrentHashMap<>();

    // If keys.properties is missing or can't be read, you'll get a big ol' IOException.
    private AppConfiguration(final Context context) {
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

    static void setupInstance(final Context context) {
        if (getInstance() == null) {
            instance = new AppConfiguration(context);
        }
    }

    static AppConfiguration getInstance() {
        return instance;
    }

    String getConfig(final String config) {
        String mconfig = config;
        if (BuildConfig.DEBUG && isDebug) { mconfig = "debug." + config; }

        String prop = props.getProperty(mconfig);
        if (prop == null) { prop = props.getProperty(config); }

        return prop;
    }

    String getSecret(final String config) {
        return secrets.getProperty(config);
    }

    void setDynamicProperty(final String key, final String value) {
        dynamicProperties.put(key, value);
    }

    String getDynamicProperty(final String key) {
        return dynamicProperties.get(key);
    }
}
