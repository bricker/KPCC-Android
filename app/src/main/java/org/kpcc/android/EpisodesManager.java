package org.kpcc.android;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Episode;

import java.util.ArrayList;
import java.util.Collections;

public class EpisodesManager {
    public static final String TAG = "EpisodesManager";
    public ArrayList<Episode> mEpisodes = new ArrayList<>();

    public String mProgramSlug;

    public EpisodesManager(String programSlug) {
        mProgramSlug = programSlug;
    }

    public void loadEpisodes() {
        RequestParams params = new RequestParams();
        params.add("program", mProgramSlug);
        params.add("limit", "8");

        Episode.Client.getCollection(params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    JSONArray jsonEpisodes = response.getJSONArray(Episode.PLURAL_KEY);

                    for (int i = 0; i < jsonEpisodes.length(); i++) {
                        Episode episode = Episode.buildFromJson(jsonEpisodes.getJSONObject(i));
                        mEpisodes.add(episode);
                    }

                    Collections.sort(mEpisodes);
                } catch (JSONException e) {
                }
            }

            @Override
            public void onFailure(String responseBody, Throwable error) {
            }
        });
    }


    public Episode find(String url) {
        Episode foundEpisode = null;

        for (Episode episode : mEpisodes) {
            if (episode.getPublicUrl().equals(url)) {
                foundEpisode = episode;
                break;
            }
        }

        return foundEpisode;
    }
}
