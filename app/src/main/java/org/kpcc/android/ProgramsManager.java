package org.kpcc.android;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kpcc.api.Program;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ProgramsManager {
    public static ProgramsManager instance = new ProgramsManager();
    private static final String[] HIDDEN_PROGRAMS = { "filmweek-marquee", "take-two-evenings" };

    public final ArrayList<Program> ALL_PROGRAMS = new ArrayList<>();

    private ProgramsManager() {
    }

    public Request loadPrograms(final OnProgramsResponseListener listener) {
        if (!ALL_PROGRAMS.isEmpty()) {
            listener.onProgramsResponse();
            return null;
        }

        HashMap<String, String> params = new HashMap<>();
        params.put("air_status", "onair");

        return Program.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonPrograms = response.getJSONArray(Program.PLURAL_KEY);
                    List<String> hiddenPrograms = Arrays.asList(HIDDEN_PROGRAMS);

                    for (int i = 0; i < jsonPrograms.length(); i++) {
                        try {
                            Program program = Program.buildFromJson(jsonPrograms.getJSONObject(i));

                            // Skip hidden programs.
                            if (!hiddenPrograms.contains(program.slug)) {
                                ALL_PROGRAMS.add(program);
                            }
                        } catch (JSONException e) {
                            // implicit continue.
                        }
                    }

                    Collections.sort(ALL_PROGRAMS);
                    listener.onProgramsResponse();
                } catch (JSONException e) {
                    // No programs will be available.
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // No programs will be available.
                // The fragment should check the status and try to reload the programs.
                listener.onProgramsError(error);
            }
        });
    }

    public Program find(String slug) {
        Program foundProgram = null;

        for (Program program : ALL_PROGRAMS) {
            if (program.slug.equals(slug)) {
                foundProgram = program;
                break;
            }
        }

        return foundProgram;
    }


    public abstract static interface OnProgramsResponseListener {
        abstract void onProgramsResponse();
        abstract void onProgramsError(VolleyError error);
    }

}
