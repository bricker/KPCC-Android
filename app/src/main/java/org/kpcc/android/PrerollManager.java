package org.kpcc.android;

import android.content.Context;
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

public class PrerollManager {
    public final static long PREROLL_THRESHOLD = 1000 * 60 * 60 * 4; // 4 hours
    public final static long INSTALL_GRACE = 1000 * 60 * 10; // 10 minutes
    public final static PrerollManager instance = new PrerollManager();
    private final static String PREROLL_BASE = "http://adserver.adtechus.com/?adrawdata/3.0/5511.1/3590533/0/0/header=yes;cookie=no;adct=text/xml;guid=%s:%s";

    public static long LAST_PREROLL_PLAY = 0;

    private PrerollCallbackListener mCallback;

    public void getPrerollData(Context context, PrerollCallbackListener callback) {
        mCallback = callback;
        new PrerollAsync().execute(context);
    }

    public abstract static class PrerollCallbackListener {
        abstract public void onPrerollResponse(PrerollData prerollData);
    }

    public static class PrerollData {
        public String audioUrl;
        public Integer audioDurationSeconds;
        public String assetUrl;
        public String assetClickUrl;
        public String trackingUrl;
        public String impressionUrl;
    }

    private static class XmlParser {
        private final String mData;

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
                        case "Impression":
                            if (prerollData.impressionUrl == null) {
                                prerollData.impressionUrl = readText(parser);
                            }
                            continue;
                        case "Duration":
                            if (prerollData.audioDurationSeconds == null) {
                                String durationString = readText(parser);
                                String[] segments = durationString.split(":");
                                int duration = 0;
                                duration += Integer.valueOf(segments[0]) * 60 * 60; // hours
                                duration += Integer.valueOf(segments[1]) * 60; // minutes
                                duration += Integer.valueOf(segments[2].split("\\.")[0]); // seconds
                                prerollData.audioDurationSeconds = duration;
                            }
                            continue;
                        case "MediaFile":
                            if (prerollData.audioUrl == null) {
                                String type = parser.getAttributeValue(null, "type");
                                if (type != null && type.matches("audio.+")) {
                                    prerollData.audioUrl = readText(parser);
                                }
                            }
                            continue;
                        case "StaticResource":
                            if (prerollData.assetUrl == null) {
                                String creativeType = parser.getAttributeValue(null, "creativeType");
                                if (creativeType != null && creativeType.matches("image.+")) {
                                    prerollData.assetUrl = readText(parser);
                                }
                            }
                            continue;
                        case "Tracking":
                            if (prerollData.trackingUrl == null) {
                                prerollData.trackingUrl = readText(parser);
                            }
                            continue;
                        case "CompanionClickThrough":
                            if (prerollData.assetClickUrl == null) {
                                prerollData.assetClickUrl = readText(parser);
                            }
                            continue;
                        default: // implicit continue
                    }
                }

            } catch (XmlPullParserException | IOException e) {
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

    private class PrerollAsync extends AsyncTask<Context, Void, AdvertisingIdClient.Info> {
        @Override
        protected AdvertisingIdClient.Info doInBackground(Context... contexts) {
            AdvertisingIdClient.Info adIdInfo = null;
            Context context = contexts[0];

            try {
                adIdInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
            } catch (GooglePlayServicesNotAvailableException | GooglePlayServicesRepairableException | IOException e) {
                // ad info will be null and no preroll will play.
            }

            if (adIdInfo != null) {
                String id = adIdInfo.getId();
                String type;

                if (id == null) {
                    type = "app";

                    id = DataManager.instance.getAdId();

                    if (id.isEmpty()) {
                        id = UUID.randomUUID().toString();
                        DataManager.instance.setAdId(id);
                    }
                } else {
                    type = "gaid";
                }

                String url = String.format(PREROLL_BASE, type, id);

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
}
