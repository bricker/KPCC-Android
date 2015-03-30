package org.kpcc.api;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Episode extends Entity implements Comparable<Episode> {
    public final static String PLURAL_KEY = "episodes";
    private final static String ENDPOINT = PLURAL_KEY;

    // API Client
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);

    public String title;
    public Date airDate;
    public String formattedAirDate;
    private String rawAirDate; // For serializing
    public String publicUrl;
    public Audio audio;
    public final ArrayList<Segment> segments = new ArrayList<>();

    public static Episode buildFromJson(String jsonEpisode) throws JSONException {
        return buildFromJson(new JSONObject(jsonEpisode));
    }

    public static Episode buildFromJson(JSONObject jsonEpisode) throws JSONException {
        // We could keep track of the full jsonEpisode object for serialization, but these
        // episodes tend to stick around a while (and there are a lot of them),
        // and if we stored the json object we'd be using up more memory than we need.
        Episode episode = new Episode();

        episode.title = jsonEpisode.getString(PROP_TITLE);
        episode.rawAirDate = jsonEpisode.getString(PROP_AIR_DATE);
        episode.airDate = parseISODate(jsonEpisode.getString(PROP_AIR_DATE));
        episode.formattedAirDate = parseHumanDate(episode.airDate);

        // URL is unique so we'll use it as ID too.
        episode.publicUrl = jsonEpisode.getString(PROP_PUBLIC_URL);

        // It's possible an episode will exist without audio. This will probably happen
        // when the show is split into segments.
        JSONArray audiosJson = jsonEpisode.getJSONArray(Audio.PLURAL_KEY);

        if (audiosJson.length() > 0) {
            // This app only uses the first audio.
            JSONObject audioJson = audiosJson.getJSONObject(0);
            if (audioJson != null) {
                episode.audio = Audio.buildFromJson(audioJson);
            }
        } else {
            // For this app, episodes and segments are treated exactly the same.
            // If an episode doesn't have any audio, but has segments, then we're going to
            // use the segments as "episodes". We only need to do this if the audio is missing.
            JSONArray jsonSegments = jsonEpisode.getJSONArray(Segment.PLURAL_KEY);
            for (int i = 0; i < jsonSegments.length(); i++) {
                JSONObject segmentJson = jsonSegments.getJSONObject(i);
                episode.segments.add(Segment.buildFromJson(segmentJson));
            }
        }

        return episode;
    }

    public static Episode buildFromSegment(Segment segment) {
        Episode episode = new Episode();

        episode.title = segment.title;
        episode.rawAirDate = segment.rawPublishedAt;
        episode.airDate = segment.publishedAt;
        episode.publicUrl = segment.publicUrl;
        episode.audio = segment.audio;
        episode.formattedAirDate = parseHumanDate(episode.airDate);

        return episode;
    }

    @Override
    public int compareTo(@NonNull Episode otherEpisode) {
        return -airDate.compareTo(otherEpisode.airDate);
    }

    // This function isn't complete - it was made for the EpisodePager and is missing some stuff
    // that isn't needed. See buildFromJson for an explanation.
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put(Entity.PROP_TITLE, title);
        json.put(Entity.PROP_PUBLIC_URL, publicUrl);
        json.put(Entity.PROP_AIR_DATE, rawAirDate);

        JSONArray audios = new JSONArray();

        if (audio != null) {
            audios.put(audio.toJSON());
        }

        json.put(Audio.PLURAL_KEY, audios);
        return json;
    }
}
