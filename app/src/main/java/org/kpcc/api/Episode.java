package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Episode extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("episodes");


    private String mTitle;
    private String mSummary;
    private Date mAirDate;
    private String mPublicUrl;
    private Program mProgram;
    private ArrayList<Asset> mAssets;
    private ArrayList<Audio> mAudio;
    private ArrayList<Article> mSegments;


    public static Episode buildFromJson(JSONObject jsonEpisode)
    {
        Episode episode = new Episode();

        try
        {
            episode.setTitle(jsonEpisode.getString("title"));
            episode.setSummary(jsonEpisode.getString("summary"));
            episode.setAirDate(parseISODate(jsonEpisode.getString("air_date")));
            episode.setPublicUrl(jsonEpisode.getString("public_url"));

            episode.setProgram(Program.buildFromJson(jsonEpisode.getJSONObject("program")));

            JSONArray assets = jsonEpisode.getJSONArray("assets");
            for (int i=0; i < assets.length(); i++)
            { episode.addAsset(Asset.buildFromJson(assets.getJSONObject(i))); }

            JSONArray audio = jsonEpisode.getJSONArray("audio");
            for (int i=0; i < audio.length(); i++)
            { episode.addAudio(Audio.buildFromJson(audio.getJSONObject(i))); }

            JSONArray segments = jsonEpisode.getJSONArray("segments");
            for (int i=0; i < segments.length(); i++)
            { episode.addSegment(Article.buildFromJson(segments.getJSONObject(i))); }

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


    public String getSummary()
    {
        return mSummary;
    }

    public void setSummary(String summary)
    {
        mSummary = summary;
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


    public ArrayList<Asset> getAssets()
    {
        return mAssets;
    }

    public void setAssets(ArrayList<Asset> assets)
    {
        mAssets = assets;
    }

    public void addAsset(Asset asset)
    {
        mAssets.add(asset);
    }

    public boolean hasAssets()
    {
        return mAssets.size() > 0;
    }


    public ArrayList<Audio> getAudio()
    {
        return mAudio;
    }

    public void setAudio(ArrayList<Audio> audio)
    {
        mAudio = audio;
    }

    public void addAudio(Audio audio)
    {
        mAudio.add(audio);
    }

    public boolean hasAudio()
    {
        return mAudio.size() > 0;
    }


    public ArrayList<Article> getSegments()
    {
        return mSegments;
    }

    public void setSegments(ArrayList<Article> segments)
    {
        mSegments = segments;
    }

    public void addSegment(Article segment)
    {
        mSegments.add(segment);
    }

    public boolean hasSegments()
    {
        return mSegments.size() > 0;
    }

}
