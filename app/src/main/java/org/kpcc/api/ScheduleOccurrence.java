package org.kpcc.api;

import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class ScheduleOccurrence extends Entity {
    private final static String ENDPOINT = "schedule";
    public final static ApiClient Client = new ApiClient(ENDPOINT);
    public final static String SINGULAR_KEY = "schedule_occurrence";
    private final static String CURRENT_ENDPOINT = "current";

    public String title;
    public Date softStartsAt;
    public String programSlug;


    public static ScheduleOccurrence buildFromJson(JSONObject jsonSchedule) throws JSONException {
        if (jsonSchedule.length() == 0) {
            return null;
        }

        ScheduleOccurrence schedule = new ScheduleOccurrence();

        schedule.title = jsonSchedule.getString(PROP_TITLE);
        schedule.softStartsAt = parseISODateTime(jsonSchedule.getString(PROP_SOFT_STARTS_AT));

        if (jsonSchedule.has(Program.SINGULAR_KEY)) {
            schedule.programSlug = jsonSchedule.getJSONObject(Program.SINGULAR_KEY).getString(PROP_SLUG);
        }

        return schedule;
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
