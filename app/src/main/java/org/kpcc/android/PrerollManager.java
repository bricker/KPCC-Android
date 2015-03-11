package org.kpcc.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Xml;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;

/**
 * Created by rickb014 on 3/3/15.
 */
public class PrerollManager {
    private final static PrerollManager INSTANCE = new PrerollManager();

    private final static long PREROLL_THRESHOLD = 600L;
    public final static String TRITON_BASE = "http://cmod.live.streamtheworld.com/ondemand/ars?type=preroll&stid=83153&lsid=%s:%s";

    private PrerollCallbackListener mCallback;

    protected PrerollManager() {}

    public static PrerollManager getInstance() {
        return INSTANCE;
    }

    public void getPrerollData(Context context, PrerollCallbackListener callback) {
        mCallback = callback;
        new PrerollAsync().execute(context);
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
                        editor.apply();
                    }
                } else {
                    type = "gaid";
                }

                String url = String.format(TRITON_BASE, type, id);

                HttpRequest.XmlRequest.get(url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        XmlParser parser = new XmlParser(response);
                        mCallback.onPrerollResponse(parser.getPrerollData());
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // No Preroll will be played.
                        mCallback.onPrerollResponse(null);
                    }
                });
            }

            return adIdInfo;
        }
    }

    public abstract static class PrerollCallbackListener {
        abstract public void onPrerollResponse(PrerollData prerollData);
    }

    public static class PrerollData {
        private String mAudioUrl;
        private Integer mAudioDuration; // Seconds
        private String mAssetUrl;
        private String mAssetClickUrl;
        private Integer mAssetWidth;
        private Integer mAssetHeight;

        public void setAudioUrl(String audioUrl) {
            mAudioUrl = audioUrl;
        }

        public String getAudioUrl() {
            return mAudioUrl;
        }

        public Integer getAudioDuration() {
            return mAudioDuration;
        }

        public void setAudioDuration(Integer audioDuration) {
            mAudioDuration = audioDuration;
        }

        public void setAssetUrl(String assetUrl) {
            mAssetUrl = assetUrl;
        }

        public String getAssetUrl() {
            return mAssetUrl;
        }

        public String getAssetClickUrl() {
            return mAssetClickUrl;
        }

        public void setAssetClickUrl(String assetClickUrl) {
            mAssetClickUrl = assetClickUrl;
        }

        public Integer getAssetWidth() {
            return mAssetWidth;
        }

        public void setAssetWidth(Integer assetWidth) {
            mAssetWidth = assetWidth;
        }

        public Integer getAssetHeight() {
            return mAssetHeight;
        }

        public void setAssetHeight(Integer assetHeight) {
            mAssetHeight = assetHeight;
        }
    }

    private static class XmlParser {
        private String mData;

        public XmlParser(String data) {
            mData = data;
        }

        public PrerollData getPrerollData() {
            PrerollData prerollData = new PrerollData();

            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
                parser.setInput(new StringReader(mData));

                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }

                    String name = parser.getName();
                    switch (name) {
                        case "VAST":
                            continue;
                        case "Ad":
                            continue;
                        case "InLine":
                            continue;
                        case "AdSystem":
                            continue;
                        case "AdTitle":
                            continue;
                        case "Impression":
                            continue;
                        case "Creatives":
                            continue;
                        case "Creative":
                            continue;
                        case "Linear":
                            continue;
                        case "MediaFiles":
                            continue;
                        case "Duration":
                            if (prerollData.getAudioDuration() != null) { continue; }
                            String durationString = readText(parser);
                            String[] segments = durationString.split(":");
                            int duration = 0;
                            duration += Integer.valueOf(segments[0]) * 60 * 60; // hours
                            duration += Integer.valueOf(segments[1]) * 60; // minutes
                            duration += Integer.valueOf(segments[2].split("\\.")[0]); // seconds
                            prerollData.setAudioDuration(duration);
                            continue;
                        case "MediaFile":
                            if (prerollData.getAudioUrl() != null) { continue; }
                            String type = parser.getAttributeValue(null, "type");
                            if (type != null && type.matches("audio.+")) {
                                prerollData.setAudioUrl(readText(parser));
                            }
                            continue;
                        case "CompanionAds":
                            continue;
                        case "Companion":
                            if (prerollData.getAssetUrl() != null) { continue; }
                            String width = parser.getAttributeValue(null, "width");
                            String height = parser.getAttributeValue(null, "height");
                            if (width != null) {
                                prerollData.setAssetWidth(Integer.valueOf(width));
                            }
                            if (height != null) {
                                prerollData.setAssetHeight(Integer.valueOf(height));
                            }
                            continue;
                        case "StaticResource":
                            if (prerollData.getAssetUrl() != null) { continue; }
                            String creativeType = parser.getAttributeValue(null, "creativeType");
                            if (creativeType != null && creativeType.matches("image.+")) {
                                prerollData.setAssetUrl(readText(parser));
                            }
                            continue;
                        case "TrackingEvents":
                            continue;
                        case "Tracking":
                            continue;
                        case "CompanionClickThrough":
                            if (prerollData.getAssetClickUrl() != null) { continue; }
                            prerollData.setAssetClickUrl(readText(parser));
                            continue;
                        case "AltText":
                            continue;
                        case "HTMLResource":
                            continue;
                        case "IFrameResource":
                            continue;
                        default: // implicit continue
                    }
                }

            } catch (XmlPullParserException | IOException e) {
                String string = e.toString();
                return null; // Preroll will just be skipped
            }

            return prerollData;
        }

        private String readText(XmlPullParser parser) throws XmlPullParserException, IOException {
            String result = "";

            if (parser.next() == XmlPullParser.TEXT) {
                result = parser.getText().trim();
                parser.nextTag();
            }

            return result;
        }
    }
}
