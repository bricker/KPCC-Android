package org.kpcc.android;

import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ProgramsManager {
    private static ProgramsManager INSTANCE = null;

    public static final String TAG = "ProgramsManager";
    public static ArrayList<Program> ALL_PROGRAMS = new ArrayList<Program>();
    public static final String PROGRAM_TILE_URL = "http://media.scpr.org/iphone/program-images/program_tile_%s@2x.jpg";

    public static String buildTileUrl(String slug) {
        return String.format(ProgramsManager.PROGRAM_TILE_URL, slug);
    }

    protected ProgramsManager() {
        HashMap<String, String> params = new HashMap<>();
        params.put("air_status", "onair");

        Program.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "programs success");
                // TODO: Download images too

                try {
                    JSONArray jsonPrograms = response.getJSONArray(Program.PLURAL_KEY);

                    for (int i = 0; i < jsonPrograms.length(); i++) {
                        Program program = Program.buildFromJson(jsonPrograms.getJSONObject(i));
                        ALL_PROGRAMS.add(program);
                    }

                    Collections.sort(ALL_PROGRAMS);
                } catch (JSONException e) {
                    // TODO: Handle errors
                    Log.e(TAG, "ERROR");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO: Handle response errors
                Log.d(TAG, "programs failure");
            }
        });
    }

    public static ProgramsManager getInstance() {
        return INSTANCE;
    }

    public static void setupInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ProgramsManager();
        }
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
