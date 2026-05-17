package com.example.neighbourneed;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

class CustomerApi {

    static final String BASE_URL = "https://wmc.ms.wits.ac.za/students/sgroup2677/PHP/";

    private final OkHttpClient client = new OkHttpClient();

    interface ApiCallback {
        void onResult(boolean networkSuccess, String responseText);
    }

    void post(String endpoint, FormBody.Builder formBuilder, ApiCallback callback) {
        RequestBody requestBody = formBuilder.build();
        Request request = new Request.Builder()
                .url(BASE_URL + endpoint)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onResult(false, errorJson("Network error. Check your connection."));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    callback.onResult(false, errorJson("Empty server response from " + endpoint));
                    return;
                }

                String responseText = response.body().string().trim();
                if (!response.isSuccessful()) {
                    String message = response.code() == 404
                            ? "Server file not found: " + endpoint + ". Upload this PHP file to the Wits PHP folder."
                            : "Server error (" + response.code() + ") while loading " + endpoint;
                    callback.onResult(false, errorJson(message));
                    return;
                }

                if (!responseText.startsWith("{")) {
                    callback.onResult(false, errorJson("Invalid server response from " + endpoint + ". Check that the PHP file is uploaded and returns JSON."));
                    return;
                }

                callback.onResult(true, responseText);
            }
        });
    }

    private static String errorJson(String message) {
        try {
            return new JSONObject()
                    .put("success", false)
                    .put("message", message)
                    .toString();
        } catch (JSONException e) {
            return "{\"success\":false,\"message\":\"Server error\"}";
        }
    }
}
