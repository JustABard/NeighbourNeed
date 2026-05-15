package com.example.neighbourneed.data;

import com.example.neighbourneed.data.model.LoggedInUser;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
public class LoginDataSource {

    private static final String LOGIN_URL = "https://wmc.ms.wits.ac.za/students/sgroup2677/PHP/login.php";
    private final OkHttpClient client = new OkHttpClient();

    public Result<LoggedInUser> login(String username, String password) {

        try {
            RequestBody requestBody = new FormBody.Builder()
                    .add("email", username)
                    .add("password", password)
                    .build();

            Request request = new Request.Builder()
                    .url(LOGIN_URL)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return new Result.Error(new IOException("Server error"));
                }

                String responseText = response.body().string();
                JSONObject jsonObject = new JSONObject(responseText);

                if (!jsonObject.optBoolean("success")) {
                    String message = jsonObject.optString("message", "Invalid email or password");
                    return new Result.Error(new IOException(message));
                }

                LoggedInUser user = new LoggedInUser(
                        String.valueOf(jsonObject.optInt("user_id")),
                        jsonObject.optString("full_name")
                );
                return new Result.Success<>(user);
            }
        } catch (IOException | JSONException e) {
            return new Result.Error(new IOException("Error logging in", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}
