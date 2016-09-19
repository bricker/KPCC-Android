package org.kpcc.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;


public class Program extends Entity implements Comparable<Program> {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public final static String PLURAL_KEY = "programs";
    private final static String ENDPOINT = PLURAL_KEY;
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);
    public final static String SINGULAR_KEY = "program";

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private String title;
    private String slug;
    private String normalizedTitle;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public static Program buildFromJson(JSONObject jsonProgram) throws JSONException {
        Program program = new Program();

        program.setTitle(jsonProgram.getString(PROP_TITLE));
        program.setNormalizedTitle(program.getTitle().replaceFirst("^(The )", ""));
        program.setSlug(jsonProgram.getString(PROP_SLUG));

        return program;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Implementations
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @Override // Comparable
    public int compareTo(@NonNull Program otherProgram) {
        return getNormalizedTitle().compareTo(otherProgram.getNormalizedTitle());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters / Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getNormalizedTitle() {
        return normalizedTitle;
    }

    public void setNormalizedTitle(String normalizedTitle) {
        this.normalizedTitle = normalizedTitle;
    }
}
