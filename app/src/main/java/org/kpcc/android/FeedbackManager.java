package org.kpcc.android;

import android.util.Base64;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by rickb014 on 2/22/15.
 */
public class FeedbackManager {
    public final static String TAG = "kpcc.FeedbackManager";
    private static FeedbackManager INSTANCE = null;

    public final static String TYPE_BUG = "bug";
    public final static String TYPE_SUGGESTION = "suggestion";
    public final static String TYPE_FEEDBACK = "feedback";
    public final static String DESC_BUG = "Bug";
    public final static String DESC_SUGGESTION = "Suggestion";
    public final static String DESC_FEEDBACK = "General Feedback";

    private final static String CONTENT_TYPE = "application/json";
    private final static String DESK_ROOT = "https://kpcc.desk.com/api/v2/";
    private final static String ENDPOINT_CUSTOMERS_CREATE = "customers";
    private final static String ENDPOINT_CUSTOMERS_SEARCH = "customers/search";
    private final static String ENDPOINT_CUSTOMERS_CASES = "customers/%s/cases";
    private final static String PRIORITY_BUG = "8";
    private final static String PRIORITY_SUGGESTION = "4";
    private final static String PRIORITY_FEEDBACK = "2";
    private final static String PRIORITY_DEFAULT = "5";
    private final static String KPCC_EMAIL = "mobilefeedback@scpr.org";
    private final static String LABEL = "Android Feedback";
    private final static String DEFAULT_USER = "/api/v2/users/21318558";
    private final static String DEFAULT_GROUP = "/api/v2/groups/346862";

    private final static String TEMPLATE_SUBJECT = "%s for KPCC Android from %s";
    private final static String TEMPLATE_BODY = "%s\n\n%s\n\n" +
            "Android Version: %s\n" +
            "Device: %s %s %s\n" +
            "App Version: %s (%s)";

    private AsyncHttpClient client = new AsyncHttpClient();

    protected FeedbackManager() {
        // Currently using bricker88@gmail.com account.
        String email = AppConfiguration.getInstance().getConfig("desk.email");
        String password = AppConfiguration.getInstance().getConfig("desk.password");

        // The setBasicAuth method doesn't build the header correctly for this, so we have
        // to do it manually.
        client.addHeader("Authorization", "Basic " +
            Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP)
        );

        client.addHeader("Content-Type", CONTENT_TYPE);
        client.addHeader("Accept", CONTENT_TYPE);
    }

    public static void setupInstance() {
        if (INSTANCE == null) {
            INSTANCE = new FeedbackManager();
        }
    }
    public static FeedbackManager getInstance() {
        return INSTANCE;
    }

    private void searchCustomer(final String customerEmail,
                                final CustomerResponseCallback responseHandler) {
        Log.d(TAG, "searchCustomer");
        RequestParams params = new RequestParams();
        params.put("email", customerEmail);
        Log.d(TAG, "searchCustomer sending request...");
        client.get(getAbsoluteUrl(ENDPOINT_CUSTOMERS_SEARCH), params,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject response) {
                        Log.d(TAG, "POST /customers/search success");
                        String customerId = "";

                        try {
                            if (response.getInt("total_entries") == 0) {
                                Log.d(TAG, "No Customers Found");
                                // No need to traverse the JSON.
                                // TODO: Handle Errors. How did we get here?
                            }

                            // The API documentation specifies that emails are unique, so we
                            // can safely just get the first result.
                            customerId = String.valueOf(response
                                    .getJSONObject("_embedded")
                                    .getJSONArray("entries")
                                    .getJSONObject(0)
                                    .getInt("id")
                            );

                        } catch (JSONException e) {
                            Log.d(TAG, "JSON Error 1");
                            // TODO: Handle Errors
                        }

                        if (!customerId.isEmpty()) {
                            responseHandler.onSuccess(customerId);
                        } else {
                            Log.d(TAG, "Customer ID is empty");
                            // TODO: Handle Errors
                        }
                    }

                    @Override
                    public void onFailure(Throwable e, JSONObject errorResponse) {
                        Log.d(TAG, "POST /customers/search failure");
                        // TODO: Handle errors
                        super.onFailure(e, errorResponse);
                    }
                }
        );
    }


    private void createOrFindCustomer(final String customerName,
                                      final String customerEmail,
                                      final CustomerResponseCallback responseHandler) {
        Log.d(TAG, "createOrFindCustomer");
        try {
            // Setup Customer params.
            // We need to get the customer resource to send with the message later.
            // The most common case is that a *new* Desk customer will be sending a bug report,
            // so we'll assume this user doesn't exist in Desk yet.
            // If the user does exist, then we need to make ANOTHER request to get the user resource.
            JSONObject email = new JSONObject();
            email.put("type", "work");
            email.put("value", customerEmail);

            JSONArray emails = new JSONArray();
            emails.put(email);

            JSONObject params = new JSONObject();
            params.put("emails", emails);

            String[] names = customerName.split(" +", 2);
            // We only have a single "Name" field! But Desk.com expects a first name
            // and a last name. So we have to split it up very carefully.
            // Only a single word was entered.
            if (names.length > 0) {
                params.put("first_name", names[0]);
            }
            // Two or more words were entered.
            if (names.length > 1) {
                params.put("last_name", names[1]);
            }

            StringEntity seParams = makeParams(params);
            Log.d(TAG, "createOrFindCustomer sending request...");
            client.post(null, getAbsoluteUrl(ENDPOINT_CUSTOMERS_CREATE), seParams, CONTENT_TYPE,
                    new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(JSONObject response) {
                            Log.d(TAG, "POST /customers success");
                            String customerId = "";

                            try {
                                customerId = String.valueOf(response.getInt("id"));
                            } catch (JSONException e) {
                                Log.d(TAG, "JSON Error 2");
                                // TODO: Handle Errors
                            }

                            if (!customerId.isEmpty()) {
                                // User was just created.
                                responseHandler.onSuccess(customerId);
                            } else {
                                Log.d(TAG, "Unhandled scenario.");
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                            Log.d(TAG, "POST /customers failure.");
                            Log.d(TAG, "statusCode: " + String.valueOf(statusCode));
                            // Unprocessable Entity
                            if (statusCode == 422) {
                                Log.d(TAG, "User already exists. Searching...");
                                // User already exists.
                                // Search for a customer with this e-mail address, get the href attribute.
                                searchCustomer(customerEmail, responseHandler);
                            } else {
                                Log.d(TAG, "Unhandled error response.");
                            }
                        }
                    }
            );
        } catch (JSONException e) {
            Log.d(TAG, "JSON Error 3");
            // TODO: Handle JSONException
        }
    }


    public void sendFeedback(String type,
                             String comments,
                             String customerName,
                             String customerEmail,
                             final FeedbackCallback callback) {
        Log.d(TAG, "sendFeedback");
        try {
            final String priority;
            final String description;

            switch (type) {
                case TYPE_BUG:
                    priority = PRIORITY_BUG;
                    description = DESC_BUG;
                    break;
                case TYPE_SUGGESTION:
                    priority = PRIORITY_SUGGESTION;
                    description = DESC_SUGGESTION;
                    break;
                case TYPE_FEEDBACK:
                    priority = PRIORITY_FEEDBACK;
                    description = DESC_FEEDBACK;
                    break;
                default:
                    // We don't actually need this, but better safe than crashy.
                    priority = PRIORITY_DEFAULT;
                    description = DESC_FEEDBACK;
                    break;
            }

            String subject = String.format(TEMPLATE_SUBJECT, description, customerName);

            String body = String.format(TEMPLATE_BODY, subject, comments,
                    android.os.Build.VERSION.RELEASE,
                    android.os.Build.BRAND,
                    android.os.Build.MANUFACTURER,
                    android.os.Build.MODEL,
                    BuildConfig.VERSION_NAME,
                    String.valueOf(BuildConfig.VERSION_CODE));

            // Build the params
            JSONObject message = new JSONObject();
            message.put("direction", "in");
            message.put("status", "received");
            message.put("body", body);
            message.put("subject", subject);
            message.put("from", customerEmail);
            message.put("to", KPCC_EMAIL);

            JSONObject user = new JSONObject();
            user.put("class", "user");
            user.put("href", DEFAULT_USER);

            JSONObject group = new JSONObject();
            group.put("class", "group");
            group.put("href", DEFAULT_GROUP);

            JSONObject links = new JSONObject();
            links.put("assigned_group", group);
            links.put("assigned_user", user);

            final JSONObject params = new JSONObject();
            params.put("type", "email");
            params.put("subject", subject);
            params.put("priority", priority);
            params.put("language", "en");
            params.put("status", "open");
            params.put("message", message);

            JSONArray labels = new JSONArray();
            labels.put(LABEL);
            params.put("labels", labels);

            createOrFindCustomer(customerName, customerEmail, new CustomerResponseCallback() {
                @Override
                public void onSuccess(String customerId) {
                    String path = String.format(ENDPOINT_CUSTOMERS_CASES, customerId);
                    StringEntity seParams = makeParams(params);

                    Log.d(TAG, "sendFeedback sending request...");
                    client.post(null, getAbsoluteUrl(path), seParams, CONTENT_TYPE,
                            new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(JSONObject response) {
                                    Log.d(TAG, "POST /customers/:id/cases success");
                                    callback.onSuccess();
                                }

                                @Override
                                public void onFailure(Throwable e, JSONObject errorResponse) {
                                    Log.d(TAG, "POST /customers/:id/cases failure");
                                    callback.onFailure();
                                }
                            }
                    );
                }

                @Override
                public void onFailure() {
                    Log.d(TAG, "Customer Response failure");
                    // TODO: Handle Errors
                }
            });

        } catch (JSONException e) {
            Log.d(TAG, "JSON Error 4");
            // TODO: Handle JSONException
        }
    }

    private String getAbsoluteUrl(String relativePath) {
        return DESK_ROOT + relativePath;
    }

    private StringEntity makeParams(JSONObject params) {
        StringEntity se;
        try {
            se = new StringEntity(params.toString());
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, "Error building string entity.");
            return null;
        }

        return se;
    }

    private static interface CustomerResponseCallback {
        public void onSuccess(String customerId);
        public void onFailure();
    }

    public static interface FeedbackCallback {
        public void onSuccess();
        public void onFailure();
    }
}
