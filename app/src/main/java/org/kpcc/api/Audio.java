package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Audio extends Entity
{
    public final static String ENDPOINT = "audio";
    public final static String PLURAL_KEY = "audio";
    public final static String SINGULAR_KEY = "audio";

    public final static String PROP_URL = "url";
    public final static String PROP_DURATION = "duration";
    public final static String PROP_FILESIZE = "filesize";

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("audio");


    private String mUrl;
    private int mDurationSeconds;
    private int mFilesizeBytes;


    public static Audio buildFromJson(JSONObject jsonAudio)
    {
        Audio audio = new Audio();

        try
        {
            audio.setUrl(jsonAudio.getString(PROP_URL));
            audio.setDurationSeconds(jsonAudio.getInt(PROP_DURATION));
            audio.setFilesizeBytes(jsonAudio.getInt(PROP_FILESIZE));

        } catch(JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return audio;
    }


    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
    }


    public int getDurationSeconds()
    {
        return mDurationSeconds;
    }

    public void setDurationSeconds(int durationSeconds)
    {
        mDurationSeconds = durationSeconds;
    }


    public int getFilesizeBytes()
    {
        return mFilesizeBytes;
    }

    public void setFilesizeBytes(int filesizeBytes)
    {
        mFilesizeBytes = filesizeBytes;
    }

}
