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

class PrerollManager {
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private final static PrerollManager instance = new PrerollManager();
    private final static String PREROLL_URL = AppConfiguration.getInstance().getConfig("preroll.url");
    final static private String LAST_PREROLL_PLAY_KEY = "lastPrerollPlay";
    final static long PREROLL_THRESHOLD = 1000 * 60 * 60 * 4; // 4 hours
    final static long INSTALL_GRACE = 1000 * 60 * 10; // 10 minutes

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Variables
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private PrerollCallbackListener mCallback;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Static Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static PrerollManager getInstance() {
        return instance;
    }

    synchronized long getLastPlay() {
        String val = AppConfiguration.getInstance().getDynamicProperty(LAST_PREROLL_PLAY_KEY);
        long num = 0;
        if (val != null) {
            num = Long.parseLong(val);
        }

        return num;
    }

    synchronized void setLastPlayToNow() {
        long now = System.currentTimeMillis();
        AppConfiguration.getInstance().setDynamicProperty(LAST_PREROLL_PLAY_KEY, String.valueOf(now));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Member Functions
    ////////////////////////////////////////////////////////////////////////////////////////////////
    void getPrerollData(Context context, PrerollCallbackListener callback) {
        mCallback = callback;
        new PrerollAsync().execute(context);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Classes
    ////////////////////////////////////////////////////////////////////////////////////////////////
    static class PrerollData {
        String mAudioUrl;
        String mAudioType;
        Integer mAudioDurationSeconds;
        String mAssetUrl;
        String mAssetClickUrl;
        String mTrackingUrl;
        String mImpressionUrl;

        String getAudioUrl() {
            return mAudioUrl;
        }

        void setAudioUrl(String audioUrl) {
            mAudioUrl = audioUrl;
        }

        String getAudioType() {
            return mAudioType;
        }

        void setAudioType(String audioType) {
            mAudioType = audioType;
        }

        Integer getAudioDurationSeconds() {
            return mAudioDurationSeconds;
        }

        void setAudioDurationSeconds(Integer audioDurationSeconds) {
            mAudioDurationSeconds = audioDurationSeconds;
        }

        String getAssetUrl() {
            return mAssetUrl;
        }

        void setAssetUrl(String assetUrl) {
            mAssetUrl = assetUrl;
        }

        String getAssetClickUrl() {
            return mAssetClickUrl;
        }

        void setAssetClickUrl(String assetClickUrl) {
            mAssetClickUrl = assetClickUrl;
        }

        String getTrackingUrl() {
            return mTrackingUrl;
        }

        void setTrackingUrl(String trackingUrl) {
            mTrackingUrl = trackingUrl;
        }

        String getImpressionUrl() {
            return mImpressionUrl;
        }

        void setImpressionUrl(String impressionUrl) {
            mImpressionUrl = impressionUrl;
        }
    }

    abstract static class PrerollCallbackListener {
        abstract public void onPrerollResponse(PrerollData prerollData);
    }

    private static class XmlParser {
        private final String mData;

        XmlParser(String data) {
            mData = data;
        }

        PrerollData getPrerollData() {
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
                            if (prerollData.getImpressionUrl() == null) {
                                prerollData.setImpressionUrl(readText(parser));
                            }
                            continue;
                        case "Duration":
                            if (prerollData.getAudioDurationSeconds() == null) {
                                String durationString = readText(parser);
                                String[] segments = durationString.split(":");
                                int duration = 0;
                                duration += Integer.valueOf(segments[0]) * 60 * 60; // hours
                                duration += Integer.valueOf(segments[1]) * 60; // minutes
                                duration += Integer.valueOf(segments[2].split("\\.")[0]); // seconds
                                prerollData.setAudioDurationSeconds(duration);
                            }
                            continue;
                        case "MediaFile":
                            if (prerollData.getAudioUrl() == null) {
                                String type = parser.getAttributeValue(null, "type");
                                if (type != null && type.matches("audio.+")) {
                                    prerollData.setAudioType(type);
                                    prerollData.setAudioUrl(readText(parser));
                                }
                            }
                            continue;
                        case "StaticResource":
                            if (prerollData.getAssetUrl() == null) {
                                String creativeType = parser.getAttributeValue(null, "creativeType");
                                if (creativeType != null && creativeType.matches("image.+")) {
                                    prerollData.setAssetUrl(readText(parser));
                                }
                            }
                            continue;
                        case "Tracking":
                            if (prerollData.getTrackingUrl() == null) {
                                prerollData.setTrackingUrl(readText(parser));
                            }
                            continue;
                        case "CompanionClickThrough":
                            if (prerollData.getAssetClickUrl() == null) {
                                prerollData.setAssetClickUrl(readText(parser));
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

                    id = DataManager.getInstance().getAdId();

                    if (id.isEmpty()) {
                        id = UUID.randomUUID().toString();
                        DataManager.getInstance().setAdId(id);
                    }
                } else {
                    type = "gaid";
                }

                if (AppConfiguration.getInstance().isDebug) {
                    id = UUID.randomUUID().toString();
                    type = "app";
                }

                String url = String.format(PREROLL_URL, type, id);

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
            } else {
                mCallback.onPrerollResponse(null);
            }

            return adIdInfo;
        }
    }
}
