package org.kpcc.android;

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
    public static ProgramsManager instance;
    public ArrayList<Program> ALL_PROGRAMS = new ArrayList<>();

    protected ProgramsManager() {
        HashMap<String, String> params = new HashMap<>();
        params.put("air_status", "onair");

        Program.Client.getCollection(params, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONArray jsonPrograms = response.getJSONArray(Program.PLURAL_KEY);

                    for (int i = 0; i < jsonPrograms.length(); i++) {
                        try {
                            Program program = Program.buildFromJson(jsonPrograms.getJSONObject(i));
                            ALL_PROGRAMS.add(program);
                        } catch (JSONException e) {
                            // implicit continue.
                        }
                    }

                    Collections.sort(ALL_PROGRAMS);
                } catch (JSONException e) {
                    // No programs will be available.
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                // No programs will be available.
                // The fragment should check the status and try to reload the programs.
            }
        });
    }

    public static void setupInstance() {
        instance = new ProgramsManager();
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
}
