package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Event extends Entity
{

    public final static BaseApiClient Client = new BaseApiClient("events");

    private int mId;
    private String mTitle;
    private String mPublicUrl;
    private Date mUpdatedAt;
    private Date mStartDate;
    private Date mEndDate;
    private boolean mIsAllDay;
    private String mTeaser;
    private String mBody;
    private String mPastTenseBody;
    private String mHashtag;
    private String mType;
    private boolean mIsKpccEvent;
    private Location mLocation;
    private Sponsor mSponsor;
    private String mRsvpUrl;
    private Program mProgram;
    private ArrayList<Asset> mAssets;
    private ArrayList<Audio> mAudio;


    public static Event buildFromJson(JSONObject jsonEvent)
    {
        Event event = new Event();

        try
        {
            event.setId(jsonEvent.getInt("id"));
            event.setTitle(jsonEvent.getString("title"));
            event.setPublicUrl(jsonEvent.getString("public_url"));
            event.setUpdatedAt(parseISODate(jsonEvent.getString("updated_at")));
            event.setStartDate(parseISODate(jsonEvent.getString("start_date")));
            event.setEndDate(parseISODate(jsonEvent.getString("end_date")));
            event.setAllDay(jsonEvent.getBoolean("is_all_day"));
            event.setTeaser(jsonEvent.getString("teaser"));
            event.setBody(jsonEvent.getString("body"));
            event.setHashtag(jsonEvent.getString("hashtag"));
            event.setType(jsonEvent.getString("event_type"));
            event.setKpccEvent(jsonEvent.getBoolean("is_kpcc_event"));
            event.setLocation(Location.buildFromJson(jsonEvent.getJSONObject("location")));
            event.setSponsor(Sponsor.buildFromJson(jsonEvent.getJSONObject("sponsor")));

            if (jsonEvent.has("past_tense_body"))
            { event.setPastTenseBody(jsonEvent.getString("past_tense_body")); }

            if (jsonEvent.has("rsvp_url"))
            { event.setRsvpUrl(jsonEvent.getString("rsvp_url")); }

            if (jsonEvent.has("program"))
            { event.setProgram(Program.buildFromJson(jsonEvent.getJSONObject("program"))); }

            JSONArray assets = jsonEvent.getJSONArray("assets");
            for (int i=0; i < assets.length(); i++)
            { event.addAsset(Asset.buildFromJson(assets.getJSONObject(i))); }

            JSONArray audio = jsonEvent.getJSONArray("audio");
            for (int i=0; i < audio.length(); i++)
            { event.addAudio(Audio.buildFromJson(audio.getJSONObject(i))); }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return event;
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


    public String getPublicUrl()
    {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl)
    {
        mPublicUrl = publicUrl;
    }


    public Date getUpdatedAt() {
        return mUpdatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        mUpdatedAt = updatedAt;
    }


    public Date getStartDate()
    {
        return mStartDate;
    }

    public void setStartDate(Date startDate)
    {
        mStartDate = startDate;
    }


    public Date getEndDate()
    {
        return mEndDate;
    }

    public void setEndDate(Date endDate)
    {
        mEndDate = endDate;
    }


    public boolean isAllDay()
    {
        return mIsAllDay;
    }

    public void setAllDay(boolean isAllDay)
    {
        mIsAllDay = isAllDay;
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


    public String getPastTenseBody()
    {
        return mPastTenseBody;
    }

    public void setPastTenseBody(String pastTenseBody)
    {
        mPastTenseBody = pastTenseBody;
    }


    public String getHashtag()
    {
        return mHashtag;
    }

    public void setHashtag(String hashtag)
    {
        mHashtag = hashtag;
    }


    public String getType()
    {
        return mType;
    }

    public void setType(String type)
    {
        mType = type;
    }


    public boolean isKpccEvent()
    {
        return mIsKpccEvent;
    }

    public void setKpccEvent(boolean isKpccEvent)
    {
        mIsKpccEvent = isKpccEvent;
    }


    public Location getLocation()
    {
        return mLocation;
    }

    public void setLocation(Location location)
    {
        mLocation = location;
    }


    public Sponsor getSponsor()
    {
        return mSponsor;
    }

    public void setSponsor(Sponsor sponsor)
    {
        mSponsor = sponsor;
    }


    public String getRsvpUrl()
    {
        return mRsvpUrl;
    }

    public void setRsvpUrl(String rsvpUrl)
    {
        mRsvpUrl = rsvpUrl;
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
        this.mAssets = assets;
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
        this.mAudio = audio;
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
