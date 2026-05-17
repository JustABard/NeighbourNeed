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
                    return new Result.Error(new IOException(response.code() == 404
                            ? "login.php was not found on the server"
                            : "Server error while logging in"));
                }

                String responseText = response.body().string().trim();
                if (!responseText.startsWith("{")) {
                    return new Result.Error(new IOException("Invalid login server response. Check that login.php is uploaded and returns JSON."));
                }

                JSONObject jsonObject = new JSONObject(responseText);

                if (!jsonObject.optBoolean("success")) {
                    String message = jsonObject.optString("message", "Invalid email or password");
                    return new Result.Error(new IOException(message));
                }

                LoggedInUser user = new LoggedInUser(
                        String.valueOf(jsonObject.optInt("user_id")),
                        jsonObject.optString("full_name"),
                        jsonObject.optString("user_type", "customer")
                );
                return new Result.Success<>(user);
            }
        } catch (IOException e) {
            return new Result.Error(e);
        } catch (JSONException e) {
            return new Result.Error(new IOException("Invalid login server response", e));
        }
    }

    public void logout() {
        // TODO: revoke authentication
    }
}
