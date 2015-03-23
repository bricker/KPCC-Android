package org.kpcc.android;

import android.content.Context;
import android.net.Uri;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    public static String addQueryParams(String url, Map<String, String> params) {
        if (params == null) {
            return url;
        }

        // Volley won't build the request params for us, so we have to do it here.
        Uri.Builder builder = Uri.parse(url).buildUpon();

        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return builder.build().toString();
    }

    public static class Manager {
        public static Manager instance = null;
        private static RequestQueue mRequestQueue;

        protected Manager(Context context) {
            mRequestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }

        public static void setupInstance(Context context) {
            if (instance == null) {
                instance = new Manager(context);
            }
        }

        public <T> void addToRequestQueue(Request<T> req) {
            mRequestQueue.add(req);
        }

        public RequestQueue getRequestQueue() {
            return mRequestQueue;
        }

    }

    public static class XmlRequest {
        public static void get(String url,
                               Response.Listener<String> responseListener,
                               Response.ErrorListener errorListener) {

            StringRequest req = new StringRequest(Request.Method.GET, url, responseListener, errorListener);
            HttpRequest.Manager.instance.addToRequestQueue(req);
        }
    }

    public static class JsonRequest extends Request<JSONObject> {
        private Response.Listener<JSONObject> mListener;
        private Map<String, String> mHeaders;

        public JsonRequest(int method,
                           String url,
                           Map<String, String> headers,
                           Response.Listener<JSONObject> responseListener,
                           Response.ErrorListener errorListener) {

            super(method, url, errorListener);

            mListener = responseListener;
            mHeaders = headers == null ? new HashMap<String, String>() : headers;
        }

        public static void get(String url,
                               Map<String, String> params,
                               Map<String, String> headers,
                               Response.Listener<JSONObject> responseListener,
                               Response.ErrorListener errorListener) {

            String queryUrl = HttpRequest.addQueryParams(url, params);
            JsonRequest req = new JsonRequest(Request.Method.GET, queryUrl, headers, responseListener, errorListener);
            HttpRequest.Manager.instance.addToRequestQueue(req);
        }

        public static void post(String url,
                                JSONObject params,
                                Map<String, String> headers,
                                Response.Listener<JSONObject> responseListener,
                                Response.ErrorListener errorListener) {

            JsonObjectRequestWithHeaders req = new JsonObjectRequestWithHeaders(headers, Request.Method.POST, url, params, responseListener, errorListener);
            HttpRequest.Manager.instance.addToRequestQueue(req);
        }

        @Override
        public Map<String, String> getHeaders() {
            return mHeaders;
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data,
                        HttpHeaderParser.parseCharset(response.headers));
                return Response.success(new JSONObject(jsonString),
                        HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }

        @Override
        protected void deliverResponse(JSONObject response) {
            mListener.onResponse(response);
        }


        private static class JsonObjectRequestWithHeaders extends JsonObjectRequest {
            private Map<String, String> mHeaders = new HashMap<>();

            public JsonObjectRequestWithHeaders(Map<String, String> headers,
                                                int method,
                                                String url,
                                                JSONObject params,
                                                Response.Listener<JSONObject> listener,
                                                Response.ErrorListener errorListener) {

                super(method, url, params, listener, errorListener);

                if (headers != null) {
                    mHeaders = headers;
                }
            }

            @Override
            public Map<String, String> getHeaders() {
                return mHeaders;
            }

        }
    }

}
