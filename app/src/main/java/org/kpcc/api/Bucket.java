package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;


public class Bucket extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("buckets");


    private String mTitle;
    private String mId;
    private Date mUpdatedAt;
    private ArrayList<Article> mArticles;


    public static Bucket buildFromJson(JSONObject jsonBucket)
    {
        Bucket bucket = new Bucket();

        try
        {
            bucket.setTitle(jsonBucket.getString("title"));
            bucket.setId(jsonBucket.getString("slug"));
            bucket.setUpdatedAt(parseISODate(jsonBucket.getString("updated_at")));

            if (jsonBucket.has("articles"))
            {
                JSONArray articles = jsonBucket.getJSONArray("articles");
                for (int i=0; i < articles.length(); i++)
                { bucket.addArticle(Article.buildFromJson(articles.getJSONObject(i))); }
            }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return bucket;
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


    public Date getUpdatedAt()
    {
        return mUpdatedAt;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        mUpdatedAt = updatedAt;
    }


    public ArrayList<Article> getArticles()
    {
        return mArticles;
    }

    public void setArticles(ArrayList<Article> articles)
    {
        mArticles = articles;
    }

    public void addArticle(Article article)
    {
        mArticles.add(article);
    }

    public boolean hasArticles()
    {
        return mArticles.size() > 0;
    }

}
