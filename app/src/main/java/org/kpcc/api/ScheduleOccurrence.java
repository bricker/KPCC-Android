package org.kpcc.api;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;


public class ScheduleOccurrence extends Entity
{

    // API Client
    public final static ApiClient Client = new ApiClient("schedule");


    private String mTitle;
    private String mUrl;
    private Date mStartsAt;
    private Date mEndsAt;
    private Date mSoftStartsAt;
    private boolean mIsRecurring;
    private Program mProgram;


    public static ScheduleOccurrence buildFromJson(JSONObject jsonSchedule)
    {
        ScheduleOccurrence schedule = new ScheduleOccurrence();

        try
        {
            schedule.setTitle(jsonSchedule.getString("title"));
            schedule.setUrl(jsonSchedule.getString("public_url"));
            schedule.setIsRecurring(jsonSchedule.getBoolean("is_recurring"));
            schedule.setStartsAt(parseISODateTime(jsonSchedule.getString("starts_at")));
            schedule.setEndsAt(parseISODateTime(jsonSchedule.getString("ends_at")));
            schedule.setSoftStartsAt(parseISODateTime(jsonSchedule.getString("soft_start_at")));

            if (jsonSchedule.has("program"))
            { schedule.setProgram(Program.buildFromJson(jsonSchedule.getJSONObject("program"))); }

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


    public String getUrl()
    {
        return mUrl;
    }

    public void setUrl(String url)
    {
        mUrl = url;
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


    public boolean isRecurring()
    {
        return mIsRecurring;
    }

    public void setIsRecurring(boolean isRecurring)
    {
        mIsRecurring = isRecurring;
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
            get("current", null, handler);
        }
    }

}
