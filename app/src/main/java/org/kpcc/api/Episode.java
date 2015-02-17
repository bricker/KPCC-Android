package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Episode extends Entity
        implements Comparable<Episode>
{
    public final static String ENDPOINT = "episodes";
    public final static String PLURAL_KEY = "episodes";
    public final static String SINGULAR_KEY = "episode";

    public final static String PROP_TITLE = "title";
    public final static String PROP_AIR_DATE = "air_date";
    public final static String PROP_PUBLIC_URL = "public_url";

    // API Client
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);


    private String mTitle;
    private Date mAirDate;
    private String mPublicUrl;
    private Program mProgram;
    private Audio mAudio;


    public static Episode buildFromJson(JSONObject jsonEpisode)
    {
        Episode episode = new Episode();

        try
        {
            episode.setTitle(jsonEpisode.getString(PROP_TITLE));
            episode.setAirDate(parseISODate(jsonEpisode.getString(PROP_AIR_DATE)));
            episode.setPublicUrl(jsonEpisode.getString(PROP_PUBLIC_URL));

            episode.setProgram(Program.buildFromJson(jsonEpisode.getJSONObject(Program.SINGULAR_KEY)));

            // This app only uses the first audio.
            JSONObject audioJson = jsonEpisode.getJSONArray(Audio.PLURAL_KEY).getJSONObject(0);
            if (audioJson != null) {
                episode.setAudio(Audio.buildFromJson(audioJson));
            }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return episode;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }


    public Date getAirDate()
    {
        return mAirDate;
    }

    public void setAirDate(Date airDate)
    {
        mAirDate = airDate;
    }


    public String getPublicUrl()
    {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        mPublicUrl = publicUrl;
    }


    public Program getProgram()
    {
        return mProgram;
    }

    public void setProgram(Program program)
    {
        mProgram = program;
    }


    public Audio getAudio()
    {
        return mAudio;
    }

    public void setAudio(Audio audio)
    {
        mAudio = audio;
    }


    @Override
    public int compareTo(@NonNull Episode otherEpisode) {
        return -getAirDate().compareTo(otherEpisode.getAirDate());
    }

}
