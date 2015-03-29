package org.kpcc.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;


public class Program extends Entity implements Comparable<Program> {
    public final static String PLURAL_KEY = "programs";
    public final static String ENDPOINT = PLURAL_KEY;
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);
    public final static String SINGULAR_KEY = "program";

    public String title;
    public String slug;
    public String publicUrl;
    public String normalizedTitle;


    public static Program buildFromJson(JSONObject jsonProgram) throws JSONException {
        Program program = new Program();

        program.title = jsonProgram.getString(PROP_TITLE);
        program.normalizedTitle = program.title.replaceFirst("^(The )", "");
        program.slug = jsonProgram.getString(PROP_SLUG);
        program.publicUrl = jsonProgram.getString(PROP_PUBLIC_URL);

        return program;
    }

    @Override
    public int compareTo(@NonNull Program otherProgram) {
        return normalizedTitle.compareTo(otherProgram.normalizedTitle);
    }
}
