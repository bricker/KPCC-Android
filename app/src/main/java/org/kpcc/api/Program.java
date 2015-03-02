package org.kpcc.api;

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;


public class Program extends Entity
        implements Comparable<Program> {
    public final static String PLURAL_KEY = "programs";
    public final static String ENDPOINT = PLURAL_KEY;
    // API Client
    public final static BaseApiClient Client = new BaseApiClient(ENDPOINT);
    public final static String SINGULAR_KEY = "program";
    private String mTitle;
    private String mSlug;
    private String mPublicUrl;


    public static Program buildFromJson(JSONObject jsonProgram) {
        Program program = new Program();

        try {
            program.setTitle(jsonProgram.getString(PROP_TITLE));
            program.setSlug(jsonProgram.getString(PROP_SLUG));
            program.setPublicUrl(jsonProgram.getString(PROP_PUBLIC_URL));

        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return program;
    }


    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    protected String getNormalizedTitle() {
        return mTitle.replaceFirst("^(The )", "");
    }

    public String getSlug() {
        return mSlug;
    }

    public void setSlug(String slug) {
        mSlug = slug;
    }


    public String getPublicUrl() {
        return mPublicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        mPublicUrl = publicUrl;
    }


    @Override
    public int compareTo(@NonNull Program otherProgram) {
        return getNormalizedTitle().compareTo(otherProgram.getNormalizedTitle());
    }
}
