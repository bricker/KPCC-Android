package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Segment extends Entity {
    public final static String PLURAL_KEY = "segments";

    private String mTitle;
    private Date mPublishedAt;
    private String mPublicUrl;
    private Audio mAudio;

    public static Segment buildFromJson(JSONObject jsonSegment) {
        Segment segment = new Segment();

        try {
            segment.setTitle(jsonSegment.getString(PROP_TITLE));
            segment.setPublishedAt(parseISODateTime(jsonSegment.getString(PROP_PUBLISHED_AT)));
            segment.setPublicUrl(jsonSegment.getString(PROP_PUBLIC_URL));

            JSONArray audiosJson = jsonSegment.getJSONArray(Audio.PLURAL_KEY);

            if (audiosJson.length() > 0) {
                // This app only uses the first audio.
                JSONObject audioJson = audiosJson.getJSONObject(0);
                if (audioJson != null) {
                    segment.setAudio(Audio.buildFromJson(audioJson));
                }
            }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return segment;
    }


    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public Date getPublishedAt() {
        return mPublishedAt;
    }

    public void setPublishedAt(Date publishedAt) {
        mPublishedAt = publishedAt;
    }


    public String getPublicUrl() {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        mPublicUrl = publicUrl;
    }


    public Audio getAudio() {
        return mAudio;
    }

    public void setAudio(Audio audio) {
        mAudio = audio;
    }
}
