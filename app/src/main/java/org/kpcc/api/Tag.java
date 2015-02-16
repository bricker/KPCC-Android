package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Tag extends Entity {
    private final static String TAG = "Tag";

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("tags");


    private String mTitle;
    private String mSlug;

    public static Tag buildFromJson(JSONObject jsonTag)
    {
        Tag tag = new Tag();

        try
        {
            tag.setTitle(jsonTag.getString("title"));
            tag.setSlug(jsonTag.getString("slug"));
        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return tag;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }

}
