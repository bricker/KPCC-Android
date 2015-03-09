package org.kpcc.api;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class ScheduleOccurrence extends Entity {
    public final static String ENDPOINT = "schedule";
    public final static ApiClient Client = new ApiClient(ENDPOINT);
    public final static String SINGULAR_KEY = "schedule_occurrence";
    public final static String CURRENT_ENDPOINT = "current";
    private String mTitle;
    private Date mStartsAt;
    private Date mEndsAt;
    private Date mSoftStartsAt;
    private String mProgramSlug;


    public static ScheduleOccurrence buildFromJson(JSONObject jsonSchedule) {
        if (jsonSchedule.length() == 0) {
            return null;
        }

        ScheduleOccurrence schedule = new ScheduleOccurrence();

        try {
            schedule.setTitle(jsonSchedule.getString(PROP_TITLE));
            schedule.setStartsAt(parseISODateTime(jsonSchedule.getString(PROP_STARTS_AT)));
            schedule.setEndsAt(parseISODateTime(jsonSchedule.getString(PROP_ENDS_AT)));
            schedule.setSoftStartsAt(parseISODateTime(jsonSchedule.getString(PROP_SOFT_STARTS_AT)));

            if (jsonSchedule.has(Program.SINGULAR_KEY)) {
                schedule.setProgramSlug(jsonSchedule.getJSONObject(Program.SINGULAR_KEY).getString(PROP_SLUG));
            }

        } catch (JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return schedule;
    }


    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public Date getStartsAt() {
        return mStartsAt;
    }

    public void setStartsAt(Date startsAt) {
        mStartsAt = startsAt;
    }


    public Date getEndsAt() {
        return mEndsAt;
    }

    public void setEndsAt(Date endsAt) {
        mEndsAt = endsAt;
    }


    public Date getSoftStartsAt() {
        return mSoftStartsAt;
    }

    public void setSoftStartsAt(Date softStartsAt) {
        mSoftStartsAt = softStartsAt;
    }


    public String getProgramSlug() {
        return mProgramSlug;
    }

    public void setProgramSlug(String programSlug) {
        mProgramSlug = programSlug;
    }


    public static class ApiClient extends BaseApiClient {
        public ApiClient(String endpoint) {
            super(endpoint);
        }

        public void getCurrent(Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            get(CURRENT_ENDPOINT, null, listener, errorListener);
        }
    }

}
