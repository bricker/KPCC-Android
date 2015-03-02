package org.kpcc.api;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class ScheduleOccurrence extends Entity
{
    public final static String ENDPOINT = "schedule";
    public final static String SINGULAR_KEY = "schedule_occurrence";
    public final static String CURRENT_ENDPOINT = "current";

    // API Client
    public final static ApiClient Client = new ApiClient(ENDPOINT);


    private String mTitle;
    private Date mStartsAt;
    private Date mEndsAt;
    private Date mSoftStartsAt;
    private Program mProgram;


    public static ScheduleOccurrence buildFromJson(JSONObject jsonSchedule)
    {
        ScheduleOccurrence schedule = new ScheduleOccurrence();

        try
        {
            schedule.setTitle(jsonSchedule.getString(PROP_TITLE));
            schedule.setStartsAt(parseISODateTime(jsonSchedule.getString(PROP_STARTS_AT)));
            schedule.setEndsAt(parseISODateTime(jsonSchedule.getString(PROP_ENDS_AT)));
            schedule.setSoftStartsAt(parseISODateTime(jsonSchedule.getString(PROP_SOFT_STARTS_AT)));

            if (jsonSchedule.has(Program.SINGULAR_KEY))
            { schedule.setProgram(Program.buildFromJson(jsonSchedule.getJSONObject(Program.SINGULAR_KEY))); }

        } catch(JSONException e) {
            // TODO: Handle error
            e.printStackTrace();
        }

        return schedule;
    }


    public String getTitle()
    {
        return mTitle;
    }

    public void setTitle(String title)
    {
        mTitle = title;
    }


    public Date getStartsAt()
    {
        return mStartsAt;
    }

    public void setStartsAt(Date startsAt)
    {
        mStartsAt = startsAt;
    }


    public Date getEndsAt()
    {
        return mEndsAt;
    }

    public void setEndsAt(Date endsAt)
    {
        mEndsAt = endsAt;
    }


    public Date getSoftStartsAt() {
        return mSoftStartsAt;
    }

    public void setSoftStartsAt(Date softStartsAt) {
        mSoftStartsAt = softStartsAt;
    }


    public Program getProgram()
    {
        return mProgram;
    }

    public void setProgram(Program program)
    {
        mProgram = program;
    }


    public static class ApiClient extends BaseApiClient
    {
        public ApiClient(String endpoint) { super(endpoint); }

        public void getCurrent(AsyncHttpResponseHandler handler)
        {
            get(CURRENT_ENDPOINT, null, handler);
        }
    }

}
