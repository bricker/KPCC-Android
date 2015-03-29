package org.kpcc.api;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

abstract class Entity {
    final static String PROP_TITLE = "title";
    final static String PROP_SLUG = "slug";
    final static String PROP_PUBLIC_URL = "public_url";
    final static String PROP_URL = "url";
    final static String PROP_DURATION = "duration";
    final static String PROP_AIR_DATE = "air_date";
    final static String PROP_SOFT_STARTS_AT = "soft_starts_at";
    final static String PROP_PUBLISHED_AT = "published_at";

    private static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    private static final String HUMAN_DATE_FORMAT = "MMMM d, yyyy";

    static Date parseISODateTime(String isoDateString) {
        return parseDate(new SimpleDateFormat(ISO_DATETIME_FORMAT, Locale.US), isoDateString);
    }

    static Date parseISODate(String isoDateString) {
        return parseDate(new SimpleDateFormat(ISO_DATE_FORMAT, Locale.US), isoDateString);
    }

    static String parseHumanDate(Date date) {
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
