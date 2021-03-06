package org.kpcc.android;

import android.util.Base64;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class FeedbackManager {
    private static final FeedbackManager instance = new FeedbackManager();
    private final static String DESC_BUG = "Bug";
    private final static String DESC_SUGGESTION = "Suggestion";
    private final static String DESC_FEEDBACK = "General Feedback";
    private final static String CONTENT_TYPE = "application/json";
    private final static String DESK_ROOT = "https://kpcc.desk.com/api/v2/";
    private final static String ENDPOINT_CUSTOMERS_CREATE = "customers";
    private final static String ENDPOINT_CUSTOMERS_SEARCH = "customers/search";
    private final static String ENDPOINT_CUSTOMERS_CASES = "customers/%s/cases";
    private final static String PRIORITY_BUG = "8";
    private final static String PRIORITY_SUGGESTION = "4";
    private final static String PRIORITY_FEEDBACK = "2";
    private final static String PRIORITY_DEFAULT = "5";
    private final static String KPCC_EMAIL = AppConfiguration.getInstance().getConfig("feedback.email");
    private final static String LABEL = "Android Feedback";
    private final static String DEFAULT_USER = "/api/v2/users/21318558";
    private final static String DEFAULT_GROUP = "/api/v2/groups/346862";
    private final static String TEMPLATE_SUBJECT = "%s for KPCC Android from %s";
    private final static String TEMPLATE_BODY = "%s\n\n%s\n\n" +
            "Android Version: %s\n" +
            "Device: %s %s %s\n" +
            "App Version: %s (%s)";
    private final HashMap<String, String> mHeaders = new HashMap<>();

    private FeedbackManager() {
        String email = AppConfiguration.getInstance().getSecret("desk.email");
        String password = AppConfiguration.getInstance().getSecret("desk.password");

        // The setBasicAuth method doesn't build the header correctly for this, so we have
        // to do it manually.
        mHeaders.put("Authorization", "Basic " +
                        Base64.encodeToString((email + ":" + password).getBytes(), Base64.NO_WRAP)
        );

        mHeaders.put("Content-Type", CONTENT_TYPE);
        mHeaders.put("Accept", CONTENT_TYPE);
    }

    static FeedbackManager getInstance() {
        return instance;
    }

    private void searchCustomer(final String customerEmail,
                                final CustomerResponseCallback responseHandler) {
        HashMap<String, String> params = new HashMap<>();
        params.put("email", customerEmail);

        HttpRequest.JsonRequest.get(
                getAbsoluteUrl(ENDPOINT_CUSTOMERS_SEARCH), params, mHeaders,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String customerId = null;

                        try {
                            if (response.getInt("total_entries") > 0) {
                                // The API documentation specifies that emails are unique, so we
                                // can safely just get the first result.
                                customerId = String.valueOf(response
                                                .getJSONObject("_embedded")
                                                .getJSONArray("entries")
                                                .getJSONObject(0)
                                                .getInt("id")
                                );
                            }
                        } catch (JSONException e) {
                            // No customer ID will be handled below.
                        }

                        if (customerId != null) {
                            responseHandler.onSuccess(customerId);
                        } else {
                            responseHandler.onFailure();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        responseHandler.onFailure();
                    }
                }
        );
    }

    private void createOrFindCustomer(final String customerName,
                                      final String customerEmail,
                                      final CustomerResponseCallback responseHandler) {
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

            HttpRequest.JsonRequest.post(
                    getAbsoluteUrl(ENDPOINT_CUSTOMERS_CREATE), params, mHeaders,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            String customerId = "";

                            try {
                                customerId = String.valueOf(response.getInt("id"));
                            } catch (JSONException e) {
                                responseHandler.onFailure();
                            }

                            if (!customerId.isEmpty()) {
                                // User was just created.
                                responseHandler.onSuccess(customerId);
                            } else {
                                responseHandler.onFailure();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null && error.networkResponse.statusCode == 422) {
                                // Unprocessable Entity, User already exists.
                                // Search for a customer with this e-mail address, get the href attribute.
                                searchCustomer(customerEmail, responseHandler);
                            } else {
                                // This will cover when there's no network connection or any other
                                // error from Desk.
                                responseHandler.onFailure();
                            }
                        }
                    }
            );

        } catch (JSONException e) {
            // TODO: Handle JSONException
        }
    }

    public void sendFeedback(Type type,
                             String comments,
                             String customerName,
                             String customerEmail,
                             final FeedbackCallback callback) {
        try {
            final String priority;
            final String description;

            switch (type) {
                case BUG:
                    priority = PRIORITY_BUG;
                    description = DESC_BUG;
                    break;
                case SUGGESTION:
                    priority = PRIORITY_SUGGESTION;
                    description = DESC_SUGGESTION;
                    break;
                case FEEDBACK:
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

                    HttpRequest.JsonRequest.post(
                            getAbsoluteUrl(path), params, mHeaders,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    callback.onSuccess();
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    callback.onFailure();
                                }
                            }
                    );
                }

                @Override
                public void onFailure() {
                    callback.onFailure();
                }
            });

        } catch (JSONException e) {
            callback.onFailure();
        }
    }

    private String getAbsoluteUrl(String relativePath) {
        return DESK_ROOT + relativePath;
    }

    enum Type {BUG, FEEDBACK, SUGGESTION}

    private interface CustomerResponseCallback {
        void onSuccess(String customerId);

        void onFailure();
    }

    public interface FeedbackCallback {
        void onSuccess();

        void onFailure();
    }
}
