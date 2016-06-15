package org.kpcc.android;

import android.content.Context;
import android.util.Base64;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by rickb014 on 5/4/16.
 */

public class XFSManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String KEY_SETTINGS = "iPhoneSettings";
    public static final String SETTING_NAME = "settingName";
    public static final String KPCC_PLUS_DRIVE_END = "kpccPlusDriveEnd";
    public static final String KPCC_PLUS_DRIVE_START = "kpccPlusDriveStart";
    public static final String KPCC_PLUS_STREAM = "kpccPlusStream";
    public static final String SETTING_VALUE = "settingValue";
    private static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    private static XFSManager instance;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static void setupInstance(Context context) {
        instance = new XFSManager(context);
    }
    static XFSManager getInstance() {
        return instance;
    }

    static long parseISODateTime(String isoDateString) {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_DATETIME_FORMAT, Locale.US);
        try {
            return sdf.parse(isoDateString).getTime();
        } catch (java.text.ParseException e) {
            return 0;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private long driveStartUts;
    private long driveEndUts;
    private String driveStartString;
    private String driveEndString;
    private String pfsUrl;
    private String driveHash;
    private boolean settingsLoaded = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private XFSManager(Context context) {
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public long getDriveStartUts() {
        return driveStartUts;
    }

    public long getDriveEndUts() {
        return driveEndUts;
    }

    public String getPfsUrl() {
        if (isXfsTestMode()) {
            // The PFS URL is populated in Parse, but the stream is inactive, so we can't use it.
            return "";
        }

        return pfsUrl;
    }

    public String getDriveHash() {
        if (isXfsTestMode()) {
            return "XFS_TEST_MODE";
        }

        return driveHash;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public void loadSettings(final XFSSettingsLoadCallback cb) {
        if (settingsLoaded) {
            cb.onSettingsLoaded();
            return;
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery(KEY_SETTINGS);
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> settings, ParseException e) {
                if (e != null || settings.isEmpty()) {
                    // We act like there is no pledge drive.
                    return;
                }

                for (ParseObject setting : settings) {
                    String value = (String)setting.get(SETTING_VALUE);

                    switch ((String)setting.get(SETTING_NAME)) {
                        case KPCC_PLUS_DRIVE_START:
                            driveStartString = value;
                            driveStartUts = parseISODateTime(value);
                            break;
                        case KPCC_PLUS_DRIVE_END:
                            driveEndString = value;
                            driveEndUts = parseISODateTime(value);
                            break;
                        case KPCC_PLUS_STREAM:
                            pfsUrl = value;
                            break;
                        default:
                            // Skip this attribute
                            break;
                    }
                }

                driveHash = Base64.encodeToString((driveStartString + driveEndString + pfsUrl).getBytes(), Base64.DEFAULT);

                settingsLoaded = true;
                cb.onSettingsLoaded();
            }

        });
    }

    public boolean isDriveActive() {
        if (isXfsTestMode()) return true;

        long now = System.currentTimeMillis();
        return getPfsUrl() != null &&
                !getPfsUrl().isEmpty() &&
                getDriveStartUts() <= now &&
                now < getDriveEndUts();
    }


    private boolean isXfsTestMode() {
        return AppConfiguration.getInstance().getConfigBool("xfsTestMode");
    }

    static class XFSSettingsLoadCallback {
        void onSettingsLoaded() {
        }
    }
}
