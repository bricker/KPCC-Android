package org.kpcc.api;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;

public class Episode extends Entity
        implements Comparable<Episode> {
    public final static String PLURAL_KEY = "episodes";
    public final static String ENDPOINT = PLURAL_KEY;

    // API Client
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);

    private String mTitle;
    private Date mAirDate;
    private String mFormattedAirDate;
    private String mPublicUrl;
    private Audio mAudio;
    private ArrayList<Segment> mSegments = new ArrayList<>();

    public static Episode buildFromJson(JSONObject jsonEpisode) {
        Episode episode = new Episode();

        try {
            episode.setTitle(jsonEpisode.getString(PROP_TITLE));
            episode.setAirDate(parseISODate(jsonEpisode.getString(PROP_AIR_DATE)));
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
                    episode.addSegment(Segment.buildFromJson(segmentJson));
                }
            }

        } catch (JSONException e) {
            // TODO: Handle exception
            e.printStackTrace();
        }

        return episode;
    }

    public static Episode buildFromSegment(Segment segment) {
        Episode episode = new Episode();

        episode.setTitle(segment.getTitle());
        episode.setAirDate(segment.getPublishedAt());
        episode.setPublicUrl(segment.getPublicUrl());
        episode.setAudio(segment.getAudio());

        return episode;
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
        if (mFormattedAirDate == null && getAirDate() != null) {
            mFormattedAirDate = parseHumanDate(getAirDate());
        }

        return mFormattedAirDate;
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

    public ArrayList<Segment> getSegments() {
        return mSegments;
    }

    public void addSegment(Segment segment) {
        mSegments.add(segment);
    }

    @Override
    public int compareTo(@NonNull Episode otherEpisode) {
        return -getAirDate().compareTo(otherEpisode.getAirDate());
    }

}
