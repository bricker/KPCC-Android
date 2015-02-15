package org.kpcc.api;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

// Provides basic functionality for retrieving data from the API.
//
// Example (Basic Usage):
//
//   public class Article {
//     public static BaseApiClient Client = new BaseApiClient("articles");
//   }
//
// Example (Custom client):
//
//   public class Event {
//     public static EventApiClient Client = new EventApiClient("events");
//
//     protected class EventApiClient extends BaseApiClient {
//       public EventApiClient(String endpoint) { super(endpoint) }
//       public void getCurrentEvent() { ... }
//     }
//   }
public class BaseApiClient
{
    public static final String API_ROOT = "http://www.scpr.org/api/v3/";

    protected String mEndpoint;
    protected AsyncHttpClient client = new AsyncHttpClient();


    public BaseApiClient(String endpoint)
    {
        mEndpoint = endpoint;
    }


    public void get(
    String relativePath, RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(getAbsoluteUrl(relativePath), params, responseHandler);
    }


    public void getCollection(
    RequestParams params, AsyncHttpResponseHandler responseHandler)
    {
        client.get(getAbsoluteUrl(""), params, responseHandler);
    }


    private String getAbsoluteUrl(String relativePath)
    {
        return API_ROOT + mEndpoint + "/" + relativePath;
    }

}
