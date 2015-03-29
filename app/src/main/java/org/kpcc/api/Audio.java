package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Audio extends Entity {
    public final static String PLURAL_KEY = "audio";

    public String url;
    public int durationSeconds;


    public static Audio buildFromJson(JSONObject jsonAudio) throws JSONException {
        Audio audio = new Audio();

        audio.url = jsonAudio.getString(PROP_URL);

        try {
            audio.durationSeconds = jsonAudio.getInt(PROP_DURATION);
        } catch (JSONException e) {
            audio.durationSeconds = 0; // Ensure an integer
        }

        return audio;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject audioJson = new JSONObject();
        audioJson.put(Entity.PROP_URL, url);
        audioJson.put(Entity.PROP_DURATION, durationSeconds);

        return audioJson;
    }
}
