package org.kpcc.api;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Entity
{

    public static String ISO_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static String ISO_DATE_FORMAT = "yyyy-MM-dd";

    protected static Date parseISODateTime(String isoDateString)
    {
        return parseDate(new SimpleDateFormat(ISO_DATETIME_FORMAT), isoDateString);
    }

    protected static Date parseISODate(String isoDateString)
    {
        return parseDate(new SimpleDateFormat(ISO_DATE_FORMAT), isoDateString);
    }


    private static Date parseDate(SimpleDateFormat sdf, String isoDateString) {
        Date isoDate = null;

        try
        {
            isoDate = sdf.parse(isoDateString);
        } catch (ParseException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return isoDate;
    }
}
