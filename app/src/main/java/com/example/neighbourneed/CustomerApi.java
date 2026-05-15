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
                callback.onResult(false, "Network error");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) {
                    callback.onResult(false, "Empty server response");
                    return;
                }

                callback.onResult(response.isSuccessful(), response.body().string());
            }
        });
    }
}
