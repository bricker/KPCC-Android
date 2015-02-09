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
    private Date mStartDate;
    private Date mEndDate;
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
            schedule.setStartDate(parseISODate(jsonSchedule.getString("starts_at")));
            schedule.setEndDate(parseISODate(jsonSchedule.getString("ends_at")));

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


    public Date getStartDate()
    {
        return mStartDate;
    }

    public void setStartDate(Date startDate)
    {
        mStartDate = startDate;
    }


    public Date getEndDate()
    {
        return mEndDate;
    }

    public void setEndDate(Date endDate)
    {
        mEndDate = endDate;
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
