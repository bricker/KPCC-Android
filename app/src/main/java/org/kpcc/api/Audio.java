package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Audio extends Entity {
    public final static String PLURAL_KEY = "audio";

    private String mUrl;
    private int mDurationSeconds;


    public static Audio buildFromJson(JSONObject jsonAudio) throws JSONException {
        Audio audio = new Audio();

        // Audio URLs are less likely to be unique, so we are generating a random one.
        audio.setUrl(jsonAudio.getString(PROP_URL));

        try {
            audio.setDurationSeconds(jsonAudio.getInt(PROP_DURATION));
        } catch (JSONException e) {
            audio.setDurationSeconds(0); // Ensure an integer
        }

        return audio;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject audioJson = new JSONObject();
        audioJson.put(Entity.PROP_URL, getUrl());
        audioJson.put(Entity.PROP_DURATION, getDurationSeconds());

        return audioJson;
    }

    public String getUrl() {
        return mUrl;
    }

    private void setUrl(String url) {
        mUrl = url;
    }

    public int getDurationSeconds() {
        return mDurationSeconds;
    }

    private void setDurationSeconds(int durationSeconds) {
        mDurationSeconds = durationSeconds;
    }
}
