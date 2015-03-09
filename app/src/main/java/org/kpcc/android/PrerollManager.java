package org.kpcc.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by rickb014 on 3/3/15.
 */
public class PrerollManager {
    private final static long PREROLL_THRESHOLD = 600L;
    public final static String TRITON_BASE = "http://cmod.live.streamtheworld.com/ondemand/ars";
    public final static String AD_TYPE = "preroll";
    public final static String AD_STID = "83153";
    public final static String AD_FMT = "vast";

    public PrerollManager() {}

    public void getPreroll(Context context) {
        new PrerollAsync().execute(context);

        try {
            AdvertisingIdClient.Info adIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
            adIdInfo.getId();
        } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
            // TODO: Handle Errors
        }
    }

    private class PrerollAsync extends AsyncTask<Context, Void, AdvertisingIdClient.Info> {
        @Override
        protected AdvertisingIdClient.Info doInBackground(Context... contexts) {
            AdvertisingIdClient.Info adIdInfo = null;
            Context context = contexts[0];
            Activity activity = (Activity) context;

            try {
                adIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                // TODO: Handle Errors
            }

            if (adIdInfo != null) {
                String id = adIdInfo.getId();
                String type;

                if (id == null) {
                    type = "app";

                    SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
                    id = sharedPref.getString(activity.getString(R.string.pref_fallback_ad_uuid), "");

                    if (id.isEmpty()) {
                        id = UUID.randomUUID().toString();
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(activity.getString(R.string.pref_fallback_ad_uuid), id);
                        editor.commit();
                    }
                } else {
                    type = "gaid";
                }

                Map<String, String> params = new HashMap<>();
                params.put("type", AD_TYPE);
                params.put("stid", AD_STID);
                params.put("fmt", AD_FMT);
                params.put("lsid", type + ":" + id);


                HttpRequest.get(TRITON_BASE, params, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        return;
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO: Handle Errors
                    }
                });
            }

            return adIdInfo;
        }

        @Override
        protected void onPostExecute(AdvertisingIdClient.Info result) {

        }
    }
}
