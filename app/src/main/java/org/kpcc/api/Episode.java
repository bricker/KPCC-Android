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

    private final ArrayList<Segment> segments = new ArrayList<>();
    private String mTitle;
    private Date mAirDate;
    private String mFormattedAirDate;
    private String mPublicUrl;
    private Audio mAudio;
    private String mRawAirDate; // For serializing

    public static Episode buildFromJson(String jsonEpisode) throws JSONException {
        return buildFromJson(new JSONObject(jsonEpisode));
    }

    public static Episode buildFromJson(JSONObject jsonEpisode) throws JSONException {
        // We could keep track of the full jsonEpisode object for serialization, but these
        // episodes tend to stick around a while (and there are a lot of them),
        // and if we stored the json object we'd be using up more memory than we need.
        Episode episode = new Episode();

        episode.setTitle(jsonEpisode.getString(PROP_TITLE));
        episode.setRawAirDate(jsonEpisode.getString(PROP_AIR_DATE));
        episode.setAirDate(parseISODate(jsonEpisode.getString(PROP_AIR_DATE)));
        episode.setFormattedAirDate(parseHumanDate(episode.getAirDate()));

        // URL is unique so we'll use it as ID too.
        episode.setPublicUrl(jsonEpisode.getString(PROP_PUBLIC_URL));

        // It's possible an episode will exist without audio. This will probably happen
        // when the show is split into segments.
        JSONArray audiosJson = jsonEpisode.getJSONArray(Audio.PLURAL_KEY);

        if (audiosJson.length() > 0) {
            // This app only uses the first audio.
            JSONObject audioJson = audiosJson.getJSONObject(0);
            if (audioJson != null) {
                episode.setAudio(Audio.buildFromJson(audioJson));
            }
        } else {
            // For this app, episodes and segments are treated exactly the same.
            // If an episode doesn't have any audio, but has segments, then we're going to
            // use the segments as "episodes". We only need to do this if the audio is missing.
            JSONArray jsonSegments = jsonEpisode.getJSONArray(Segment.PLURAL_KEY);
            for (int i = 0; i < jsonSegments.length(); i++) {
                JSONObject segmentJson = jsonSegments.getJSONObject(i);
                episode.getSegments().add(Segment.buildFromJson(segmentJson));
            }
        }

        return episode;
    }

    public static Episode buildFromSegment(Segment segment) {
        Episode episode = new Episode();

        episode.setTitle(segment.title);
        episode.setRawAirDate(segment.rawPublishedAt);
        episode.setAirDate(segment.publishedAt);
        episode.setPublicUrl(segment.publicUrl);
        episode.setAudio(segment.audio);
        episode.setFormattedAirDate(parseHumanDate(episode.getAirDate()));

        return episode;
    }

    @Override
    public int compareTo(@NonNull Episode otherEpisode) {
        return -getAirDate().compareTo(otherEpisode.getAirDate());
    }

    // This function isn't complete - it was made for the EpisodePager and is missing some stuff
    // that isn't needed. See buildFromJson for an explanation.
    public JSONObject toJSON() throws JSONException {
        JSONObject json = new JSONObject();

        json.put(Entity.PROP_TITLE, getTitle());
        json.put(Entity.PROP_PUBLIC_URL, getPublicUrl());
        json.put(Entity.PROP_AIR_DATE, getRawAirDate());

        JSONArray audios = new JSONArray();

        if (getAudio() != null) {
            audios.put(getAudio().toJSON());
        }

        json.put(Audio.PLURAL_KEY, audios);
        return json;
    }

    public ArrayList<Segment> getSegments() {
        return segments;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public Date getAirDate() {
        return mAirDate;
    }

    public void setAirDate(Date airDate) {
        mAirDate = airDate;
    }

    public String getFormattedAirDate() {
        return mFormattedAirDate;
    }

    public void setFormattedAirDate(String formattedAirDate) {
        mFormattedAirDate = formattedAirDate;
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

    String getRawAirDate() {
        return mRawAirDate;
    }

    void setRawAirDate(String rawAirDate) {
        mRawAirDate = rawAirDate;
    }
}
