package org.kpcc.api;

import android.text.Spanned;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Article extends Entity
{

    private final static String TAG = "Article";

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("articles");


    private String mId;
    private String mTitle;
    private String mShortTitle;
    private String mPublicUrl;
    private Category mCategory;
    private String mByline;
    private Date mTimestamp;
    private String mTeaser;
    private String mBody;
    private Spanned mParsedBody;
    private ArrayList<Asset> mAssets = new ArrayList<Asset>();
    private ArrayList<Audio> mAudio = new ArrayList<Audio>();
    private ArrayList<Attribution> mAttributions = new ArrayList<Attribution>();

    // Build an Article from a JSON response.
    // https://github.com/SCPR/api-docs/blob/master/KPCC/v2/endpoints/articles.md
    public static Article buildFromJson(JSONObject jsonArticle)
    {
        Article article = new Article();

        try
        {
            article.setId(jsonArticle.getString("id"));
            article.setTitle(jsonArticle.getString("title"));
            article.setShortTitle(jsonArticle.getString("short_title"));
            article.setPublicUrl(jsonArticle.getString("public_url"));
            article.setByline(jsonArticle.getString("byline"));
            article.setTeaser(jsonArticle.getString("teaser"));
            article.setBody(jsonArticle.getString("body"));
            article.setTimestamp(parseISODate(jsonArticle.getString("published_at")));

            if (jsonArticle.has("category"))
            { article.setCategory(Category.buildFromJson(jsonArticle.getJSONObject("category"))); }

            JSONArray assets = jsonArticle.getJSONArray("assets");
            for (int i=0; i < assets.length(); i++)
            { article.addAsset(Asset.buildFromJson(assets.getJSONObject(i))); }

            JSONArray audio = jsonArticle.getJSONArray("audio");
            for (int i=0; i < audio.length(); i++)
            { article.addAudio(Audio.buildFromJson(audio.getJSONObject(i))); }

            JSONArray attributions = jsonArticle.getJSONArray("attributions");
            for (int i=0; i < attributions.length(); i++)
            { article.addAttribution(Attribution.buildFromJson(attributions.getJSONObject(i))); }

        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return article;
    }


    @Override
    public String toString()
    {
        return mTitle;
    }


    public String getId()
    {
        return mId;
    }

    public void setId(String id)
    {
        mId = id;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }


    public String getShortTitle()
    {
        return mShortTitle;
    }

    public void setShortTitle(String shortTitle)
    {
        mShortTitle = shortTitle;
    }


    public String getPublicUrl()
    {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        mPublicUrl = publicUrl;
    }


    public Category getCategory()
    {
        return mCategory;
    }

    public void setCategory(Category category)
    {
        mCategory = category;
    }


    public String getByline()
    {
        return mByline;
    }

    public void setByline(String byline)
    {
        mByline = byline;
    }


    public Date getTimestamp()
    {
        return mTimestamp;
    }

    public void setTimestamp(Date timestamp)
    {
        mTimestamp = timestamp;
    }


    public String getTeaser()
    {
        return mTeaser;
    }

    public void setTeaser(String teaser)
    {
        mTeaser = teaser;
    }


    public String getBody()
    {
        return mBody;
    }

    public void setBody(String body)
    {
        mBody = body;
    }


    public Spanned getParsedBody()
    {
        return mParsedBody;
    }

    public void setParsedBody(Spanned parsedBody)
    {
        mParsedBody = parsedBody;
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


    public ArrayList<Attribution> getAttributions()
    {
        return mAttributions;
    }

    public void setAttributions(ArrayList<Attribution> attributions)
    {
        mAttributions = attributions;
    }

    public void addAttribution(Attribution attribution)
    {
        mAttributions.add(attribution);
    }

    public boolean hasAttributions()
    {
        return mAttributions.size() > 0;
    }

}
