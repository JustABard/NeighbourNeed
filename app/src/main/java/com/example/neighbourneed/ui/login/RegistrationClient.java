package com.example.neighbourneed.ui.login;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class RegistrationClient {

    private static final String REGISTER_URL = "https://wmc.ms.wits.ac.za/students/sgroup2677/PHP/register.php";

    private final OkHttpClient client = new OkHttpClient();

    interface RegistrationCallback {
        void onResult(boolean success, String message);
    }

    void register(FormBody.Builder formBuilder, RegistrationCallback callback) {
        RequestBody requestBody = formBuilder.build();
        Request request = new Request.Builder()
                .url(REGISTER_URL)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onResult(false, "Registration failed");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    callback.onResult(false, "Registration failed");
                    return;
                }

                String responseText = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseText);
                    boolean success = jsonObject.optBoolean("success");
                    String message = jsonObject.optString("message", "Registration failed");
                    callback.onResult(success, message);
                } catch (JSONException e) {
                    callback.onResult(false, "Invalid server response");
                }
            }
        });
    }
}
