package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Edition extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("editions");


    private int mId;
    private String mTitle;
    private Date mPublishedAt;
    private ArrayList<Abstract> mAbstracts;


    public static Edition buildFromJson(JSONObject jsonEdition)
    {
        Edition edition = new Edition();

        try
        {
            edition.setId(jsonEdition.getInt("id"));
            edition.setTitle(jsonEdition.getString("title"));
            edition.setPublishedAt(parseISODate(jsonEdition.getString("published_at")));

            JSONArray abstracts = jsonEdition.getJSONArray("abstracts");
            for (int i=0; i < abstracts.length(); i++)
            { edition.addAbstract(Abstract.buildFromJson(abstracts.getJSONObject(i))); }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return edition;
    }


    public int getId()
    {
        return mId;
    }

    public void setId(int id)
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


    public Date getPublishedAt()
    {
        return mPublishedAt;
    }

    public void setPublishedAt(Date publishedAt)
    {
        mPublishedAt = publishedAt;
    }


    public ArrayList<Abstract> getAbstracts()
    {
        return mAbstracts;
    }

    public void setAbstracts(ArrayList<Abstract> abstracts)
    {
        mAbstracts = abstracts;
    }

    public void addAbstract(Abstract abs)
    {
        mAbstracts.add(abs);
    }

    public boolean hasAbstracts()
    {
        return mAbstracts.size() > 0;
    }

}
