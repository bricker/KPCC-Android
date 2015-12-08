package org.kpcc.api;

import com.android.volley.Request;
import com.android.volley.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class ScheduleOccurrence extends Entity {
    public final static String SINGULAR_KEY = "schedule_occurrence";
    private final static String ENDPOINT = "schedule";
    public final static ApiClient Client = new ApiClient(ENDPOINT);
    private final static String AT_ENDPOINT = "at";

    public String title;
    public long softStartsAtMs;
    public long startsAtMs;
    public long endsAtMs;
    public String programSlug;


    public static ScheduleOccurrence buildFromJson(JSONObject jsonSchedule) throws JSONException {
        if (jsonSchedule.length() == 0) {
            return null;
        }

        ScheduleOccurrence schedule = new ScheduleOccurrence();

        schedule.title = jsonSchedule.getString(PROP_TITLE);

        Date softStartsAt = parseISODateTime(jsonSchedule.getString(PROP_SOFT_STARTS_AT));
        schedule.softStartsAtMs = softStartsAt.getTime();

        Date startsAt = parseISODateTime(jsonSchedule.getString(PROP_STARTS_AT));
        schedule.startsAtMs = startsAt.getTime();

        Date endsAt = parseISODateTime(jsonSchedule.getString(PROP_ENDS_AT));
        schedule.endsAtMs = endsAt.getTime();

        if (jsonSchedule.has(Program.SINGULAR_KEY)) {
            schedule.programSlug = jsonSchedule.getJSONObject(Program.SINGULAR_KEY).getString(PROP_SLUG);
        }

        return schedule;
    }

    public int length() {
        return (int)(this.endsAtMs - this.softStartsAtMs);
    }

    public long timeSinceSoftStartMs() {
        return System.currentTimeMillis() - this.softStartsAtMs;
    }

    public static class ApiClient extends BaseApiClient {
        public ApiClient(String endpoint) {
            super(endpoint);
        }

        public Request getAtTimestamp(long uts, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            Map<String,String> params = new HashMap<>();
            params.put("time", String.valueOf(uts));
            return get(AT_ENDPOINT, params, listener, errorListener);
        }
    }

}
