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
    private String mByline;
    private Date mPublishedAt;
    private Date mUpdatedAt;
    private String mTeaser;
    private String mBody;
    private Spanned mParsedBody;
    private String mPublicUrl;
    private String mThumbnail;
    private Category mCategory;
    private ArrayList<Asset> mAssets = new ArrayList<Asset>();
    private ArrayList<Audio> mAudio = new ArrayList<Audio>();
    private ArrayList<Attribution> mAttributions = new ArrayList<Attribution>();
    private ArrayList<Tag> mTags = new ArrayList<Tag>();

    // Build an Article from a JSON response.
    // https://github.com/SCPR/api-docs/blob/master/KPCC/v3/endpoints/articles.md
    public static Article buildFromJson(JSONObject jsonArticle)
    {
        Article article = new Article();

        try
        {
            article.setId(jsonArticle.getString("id"));
            article.setTitle(jsonArticle.getString("title"));
            article.setShortTitle(jsonArticle.getString("short_title"));
            article.setByline(jsonArticle.getString("byline"));
            article.setPublishedAt(parseISODate(jsonArticle.getString("published_at")));
            article.setUpdatedAt(parseISODate(jsonArticle.getString("updated_at")));
            article.setTeaser(jsonArticle.getString("teaser"));
            article.setBody(jsonArticle.getString("body"));
            article.setPublicUrl(jsonArticle.getString("public_url"));
            article.setThumbnail(jsonArticle.getString("thumbnail"));

            if (jsonArticle.has("category"))
            { article.setCategory(Category.buildFromJson(jsonArticle.getJSONObject("category"))); }

            JSONArray assets = jsonArticle.getJSONArray("assets");
            for (int i=0; i < assets.length(); i++) {
                article.addAsset(Asset.buildFromJson(assets.getJSONObject(i)));
            }

            JSONArray audio = jsonArticle.getJSONArray("audio");
            for (int i=0; i < audio.length(); i++) {
                article.addAudio(Audio.buildFromJson(audio.getJSONObject(i)));
            }

            JSONArray attributions = jsonArticle.getJSONArray("attributions");
            for (int i=0; i < attributions.length(); i++) {
                article.addAttribution(Attribution.buildFromJson(attributions.getJSONObject(i)));
            }

            JSONArray tags = jsonArticle.getJSONArray("tags");
            for (int i=0; i < tags.length(); i++) {
                article.addTag(Tag.buildFromJson(tags.getJSONObject(i)));
            }
        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return article;
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

    public String getThumbnail() {
        return mThumbnail;
    }

    public void setThumbnail(String thumbnail) {
        mThumbnail = thumbnail;
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


    public Date getPublishedAt()
    {
        return mPublishedAt;
    }

    public void setPublishedAt(Date publishedAt)
    {
        mPublishedAt = publishedAt;
    }

    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        mUpdatedAt = updatedAt;
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


    public ArrayList<Tag> getTags()
    {
        return mTags;
    }

    public void setTags(ArrayList<Tag> tags)
    {
        mTags = tags;
    }

    public void addTag(Tag tag)
    {
        mTags.add(tag);
    }

    public boolean hasTags()
    {
        return mTags.size() > 0;
    }

}
