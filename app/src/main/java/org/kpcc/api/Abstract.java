package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Abstract extends Entity
{

    private String mSource;
    private String mUrl;
    private String mHeadline;
    private String mSummary;
    private Date mArticlePublishedAt;
    private ArrayList<Asset> mAssets;
    private ArrayList<Audio> mAudio;
    private Category mCategory;


    public static Abstract buildFromJson(JSONObject jsonAbstract)
    {
        Abstract abs = new Abstract();

        try
        {
            abs.setSource(jsonAbstract.getString("source"));
            abs.setUrl(jsonAbstract.getString("url"));
            abs.setHeadline(jsonAbstract.getString("headline"));
            abs.setSummary(jsonAbstract.getString("summary"));
            abs.setArticlePublishedAt(parseISODate(jsonAbstract.getString("article_published_at")));

            if (jsonAbstract.has("category"))
            { abs.setCategory(Category.buildFromJson(jsonAbstract.getJSONObject("category"))); }

            JSONArray assets = jsonAbstract.getJSONArray("assets");
            for (int i=0; i < assets.length(); i++)
            { abs.addAsset(Asset.buildFromJson(assets.getJSONObject(i))); }

            JSONArray audio = jsonAbstract.getJSONArray("audio");
            for (int i=0; i < audio.length(); i++)
            { abs.addAudio(Audio.buildFromJson(audio.getJSONObject(i))); }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return abs;
    }


    public String getSource()
    {
        return mSource;
    }

    public void setSource(String source)
    {
        mSource = source;
    }


    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
    }


    public String getHeadline()
    {
        return mHeadline;
    }

    public void setHeadline(String headline)
    {
        mHeadline = headline;
    }


    public String getSummary()
    {
        return mSummary;
    }

    public void setSummary(String summary)
    {
        mSummary = summary;
    }


    public Date getArticlePublishedAt()
    {
        return mArticlePublishedAt;
    }

    public void setArticlePublishedAt(Date articlePublishedAt)
    {
        mArticlePublishedAt = articlePublishedAt;
    }


    public Category getCategory()
    {
        return mCategory;
    }

    public void setCategory(Category category)
    {
        mCategory = category;
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

}
