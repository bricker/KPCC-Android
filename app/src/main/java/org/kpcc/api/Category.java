package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Category extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("categories");


    private int mId;
    private String mSlug;
    private String mTitle;
    private String mPublicUrl;


    public static Category buildFromJson(JSONObject jsonCategory)
    {
        Category category = new Category();

        try
        {
            category.setId(jsonCategory.getInt("id"));
            category.setSlug(jsonCategory.getString("slug"));
            category.setTitle(jsonCategory.getString("title"));
            category.setPublicUrl(jsonCategory.getString("public_url"));

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return category;
    }


    public int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
    }


    public String getSlug()
    {
        return mSlug;
    }

    public void setSlug(String slug)
    {
        mSlug = slug;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
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
