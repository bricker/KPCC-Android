package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Sponsor extends Entity
{

    private String mTitle;
    private String mUrl;


    public static Sponsor buildFromJson(JSONObject jsonSponsor)
    {
        Sponsor sponsor = new Sponsor();

        try
        {
            sponsor.setTitle(jsonSponsor.getString("title"));
            sponsor.setUrl(jsonSponsor.getString("url"));

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return sponsor;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }

    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
    }

}
