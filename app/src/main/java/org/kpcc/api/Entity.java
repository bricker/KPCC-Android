package org.kpcc.api;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class Entity {
    public final static String PROP_TITLE = "title";
    public final static String PROP_SLUG = "slug";
    public final static String PROP_PUBLIC_URL = "public_url";
    public final static String PROP_URL = "url";
    public final static String PROP_DURATION = "duration";
    public final static String PROP_AIR_DATE = "air_date";
    public final static String PROP_STARTS_AT = "starts_at";
    public final static String PROP_ENDS_AT = "ends_at";
    public final static String PROP_SOFT_STARTS_AT = "soft_starts_at";
    public final static String PROP_PUBLISHED_AT = "published_at";

    public static String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static String HUMAN_DATE_FORMAT = "MMMM d, yyyy";

    protected static Date parseISODateTime(String isoDateString) {
        return parseDate(new SimpleDateFormat(ISO_DATETIME_FORMAT, Locale.US), isoDateString);
    }

    protected static Date parseISODate(String isoDateString) {
        return parseDate(new SimpleDateFormat(ISO_DATE_FORMAT, Locale.US), isoDateString);
    }

    protected static String parseHumanDate(Date date) {
        if (date == null) {
            return null;
        }

        return new SimpleDateFormat(HUMAN_DATE_FORMAT, Locale.US).format(date);
    }

    private static Date parseDate(SimpleDateFormat sdf, String isoDateString) {
        Date isoDate = null;

        try {
            isoDate = sdf.parse(isoDateString);
        } catch (ParseException e) {
            // date will be null.
        }

        return isoDate;
    }
}
