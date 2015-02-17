package org.kpcc.android;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;

import java.util.ArrayList;
import java.util.Collections;

public class ProgramsManager {
    private static ProgramsManager instance = null;
    public static final String TAG = "ProgramsManager";
    public static ArrayList<Program> ALL_PROGRAMS = new ArrayList<Program>();

    public static ProgramsManager getInstance() {
        return instance;
    }

    public static void setupInstance(Context context) {
        if (instance == null) {
            instance = new ProgramsManager(context);
        }
    }


    protected ProgramsManager(Context context) {
        loadAllPrograms();
    }

    public void loadAllPrograms() {
        RequestParams params = new RequestParams();
        params.add("air_status", "onair");

        Program.Client.getCollection(params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(JSONObject response) {
                // TODO: Download images too

                try {
                    JSONArray jsonPrograms = response.getJSONArray(Program.PLURAL_KEY);

                    for (int i=0; i < jsonPrograms.length(); i++) {
                        Program program = Program.buildFromJson(jsonPrograms.getJSONObject(i));
                        ALL_PROGRAMS.add(program);
                    }

                    Collections.sort(ALL_PROGRAMS);
                } catch (JSONException e) {
                    // TODO: Handle errors
                    Log.e(TAG, "ERROR");
                }
            }

            @Override
            public void onFailure(String responseBody, Throwable error) {
                // TODO: Handle response errors
                super.onFailure(responseBody, error);
            }
        });
    }


    public Program find(String slug) {
        Program foundProgram = null;

        for (Program program : ALL_PROGRAMS) {
            if (program.getSlug().equals(slug)) {
                foundProgram = program;
                break;
            }
        }

        return foundProgram;
    }
}
