package org.kpcc.api;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class Alert extends Entity
{

    // API Client
    public final static BaseApiClient Client = new BaseApiClient("alerts");


    private int mId;
    private String mHeadline;
    private String mType;
    private Date mPublishedAt;
    private String mPublicUrl;
    private String mTeaser;
    private boolean mIsMobileNotificationSent;
    private boolean mIsEmailNotificationSent;


    public static Alert buildFromJson(JSONObject jsonAlert)
    {
        Alert alert = new Alert();

        try
        {
            alert.setId(jsonAlert.getInt("id"));
            alert.setHeadline(jsonAlert.getString("headline"));
            alert.setType(jsonAlert.getString("type"));
            alert.setIsMobileNotificationSent(jsonAlert.getBoolean("mobile_notification_sent"));
            alert.setIsEmailNotificationSent(jsonAlert.getBoolean("email_notification_sent"));
            alert.setPublishedAt(parseISODate(jsonAlert.getString("published_at")));

            if (jsonAlert.has("public_url")) alert.setPublicUrl(jsonAlert.getString("public_url"));
            if (jsonAlert.has("teaser")) alert.setTeaser(jsonAlert.getString("teaser"));

        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return alert;
    }


    public int getId()
    {
        return mId;
    }

    public void setId(int id)
    {
        mId = id;
    }


    public String getHeadline()
    {
        return mHeadline;
    }

    public void setHeadline(String headline)
    {
        mHeadline = headline;
    }


    public String getType()
    {
        return mType;
    }

    public void setType(String type)
    {
        mType = type;
    }


    public Date getPublishedAt()
    {
        return mPublishedAt;
    }

    public void setPublishedAt(Date publishedAt)
    {
        mPublishedAt = publishedAt;
    }


    public String getPublicUrl()
    {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        mPublicUrl = publicUrl;
    }


    public String getTeaser()
    {
        return mTeaser;
    }

    public void setTeaser(String teaser)
    {
        mTeaser = teaser;
    }


    public boolean isMobileNotificationSent()
    {
        return mIsMobileNotificationSent;
    }

    public void setIsMobileNotificationSent(boolean isMobileNotificationSent)
    {
        mIsMobileNotificationSent = isMobileNotificationSent;
    }


    public boolean isEmailNotificationSent()
    {
        return mIsEmailNotificationSent;
    }

    public void setIsEmailNotificationSent(boolean isEmailNotificationSent)
    {
        mIsEmailNotificationSent = isEmailNotificationSent;
    }

}
