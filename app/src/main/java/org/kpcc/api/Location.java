package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Location
{

    private String mTitle;
    private String mUrl;
    private Address mAddress;


    public static Location buildFromJson(JSONObject jsonLocation)
    {
        Location location = new Location();

        try
        {
            location.setTitle(jsonLocation.getString("title"));
            location.setUrl(jsonLocation.getString("url"));
            location.setAddress(Address.buildFromJson(jsonLocation.getJSONObject("address")));

        } catch (JSONException e) {
            // TODO: Handle Exception
            e.printStackTrace();
        }

        return location;
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

    public Address getAddress()
    {
        return mAddress;
    }

    public void setAddress(Address address)
    {
        mAddress = address;
    }

}
