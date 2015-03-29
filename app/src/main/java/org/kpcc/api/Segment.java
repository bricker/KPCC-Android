package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Segment extends Entity {
    public final static String PLURAL_KEY = "segments";

    public String title;
    public Date publishedAt;
    public String rawPublishedAt;
    public String publicUrl;
    public Audio audio;

    public static Segment buildFromJson(JSONObject jsonSegment) throws JSONException {
        Segment segment = new Segment();

        segment.title = jsonSegment.getString(PROP_TITLE);
        segment.rawPublishedAt = jsonSegment.getString(PROP_PUBLISHED_AT);
        segment.publishedAt = parseISODateTime(jsonSegment.getString(PROP_PUBLISHED_AT));
        segment.publicUrl = jsonSegment.getString(PROP_PUBLIC_URL);

        JSONArray audiosJson = jsonSegment.getJSONArray(Audio.PLURAL_KEY);

        if (audiosJson.length() > 0) {
            // This app only uses the first audio.
            JSONObject audioJson = audiosJson.getJSONObject(0);
            if (audioJson != null) {
                segment.audio = Audio.buildFromJson(audioJson);
            }
        }

        return segment;
    }
}
