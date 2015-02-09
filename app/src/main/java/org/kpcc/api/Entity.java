package org.kpcc.api;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Entity
{

    public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";

    protected static Date parseISODate(String isoDateString)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
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
