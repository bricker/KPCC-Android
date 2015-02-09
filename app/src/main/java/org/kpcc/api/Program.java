package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;


public class Program extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("programs");


    private String mTitle;
    private String mId;
    private String mHost;
    private String mAirStatus;
    private String mTwitterHandle;
    private String mAirtime;
    private String mDescription;
    private String mPodcastUrl;
    private String mRssUrl;
    private String mPublicUrl;


    public static Program buildFromJson(JSONObject jsonProgram)
    {
        Program program = new Program();

        try
        {
            program.setTitle(jsonProgram.getString("title"));
            program.setId(jsonProgram.getString("slug"));
            program.setHost(jsonProgram.getString("host"));
            program.setAirStatus(jsonProgram.getString("air_status"));
            program.setAirtime(jsonProgram.getString("airtime"));
            program.setDescription(jsonProgram.getString("description"));
            program.setPublicUrl(jsonProgram.getString("public_url"));


            if (jsonProgram.has("twitter_handle"))
            { program.setTwitterHandle(jsonProgram.getString("twitter_handle")); }

            if (jsonProgram.has("podcast_url"))
            { program.setPodcastUrl(jsonProgram.getString("podcast_url")); }

            if (jsonProgram.has("rss_url"))
            { program.setRssUrl(jsonProgram.getString("rss_url")); }

        } catch(JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return program;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }


    public String getId()
    {
        return mId;
    }

    public void setId(String id)
    {
        mId = id;
    }


    public String getHost()
    {
        return mHost;
    }

    public void setHost(String host)
    {
        mHost = host;
    }


    public String getAirStatus()
    {
        return mAirStatus;
    }

    public void setAirStatus(String airStatus)
    {
        mAirStatus = airStatus;
    }


    public String getTwitterHandle()
    {
        return mTwitterHandle;
    }

    public void setTwitterHandle(String twitterHandle)
    {
        mTwitterHandle = twitterHandle;
    }


    public String getAirtime()
    {
        return mAirtime;
    }

    public void setAirtime(String airtime)
    {
        mAirtime = airtime;
    }


    public String getDescription()
    {
        return mDescription;
    }

    public void setDescription(String description)
    {
        mDescription = description;
    }


    public String getPodcastUrl()
    {
        return mPodcastUrl;
    }

    public void setPodcastUrl(String podcastUrl)
    {
        mPodcastUrl = podcastUrl;
    }


    public String getRssUrl()
    {
        return mRssUrl;
    }

    public void setRssUrl(String rssUrl)
    {
        mRssUrl = rssUrl;
    }


    public String getPublicUrl()
    {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        mPublicUrl = publicUrl;
    }

}
