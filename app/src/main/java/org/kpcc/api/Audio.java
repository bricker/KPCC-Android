package org.kpcc.api;

import org.json.JSONException;
import org.json.JSONObject;

public class Audio extends Entity {
    public final static String PLURAL_KEY = "audio";

    public String url;
    public int durationSeconds;


    public static Audio buildFromJson(JSONObject jsonAudio) {
        Audio audio = new Audio();

        try {
            audio.url = jsonAudio.getString(PROP_URL);
            audio.durationSeconds = jsonAudio.getInt(PROP_DURATION);
        } catch (JSONException e) {
            // TODO: Handle error
        }

        return audio;
    }
}
