package org.kpcc.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

public class Segment extends Entity {
    public final static String PLURAL_KEY = "segments";

    public String title;
    public Date publishedAt;
    public String publicUrl;
    public Audio audio;

    public static Segment buildFromJson(JSONObject jsonSegment) {
        Segment segment = new Segment();

        try {
            segment.title = jsonSegment.getString(PROP_TITLE);
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

        } catch (JSONException e) {
            // TODO: Handle exception
        }

        return segment;
    }
}
