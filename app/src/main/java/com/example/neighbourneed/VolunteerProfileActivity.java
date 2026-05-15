package com.example.neighbourneed;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class VolunteerProfileActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private TextView profileHeader;
    private LinearLayout thanksList;
    private EditText thanksMessage;
    private Button postThanksButton;
    private String shopperUserId;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);

        sessionManager = new SessionManager(this);
        shopperUserId = getIntent().getStringExtra("shopper_user_id");
        orderId = getIntent().getStringExtra("order_id");

        profileHeader = findViewById(R.id.volunteer_profile_header);
        thanksList = findViewById(R.id.thanks_list);
        thanksMessage = findViewById(R.id.thanks_message);
        postThanksButton = findViewById(R.id.post_thanks);

        postThanksButton.setVisibility("customer".equals(sessionManager.getUserType()) ? View.VISIBLE : View.GONE);
        thanksMessage.setVisibility("customer".equals(sessionManager.getUserType()) ? View.VISIBLE : View.GONE);
        postThanksButton.setOnClickListener(view -> postThanks());

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadProfile() {
        if (shopperUserId == null || shopperUserId.isEmpty()) {
            profileHeader.setText("Volunteer not found");
            return;
        }

        api.post("volunteer_profile.php",
                new FormBody.Builder().add("shopper_user_id", shopperUserId),
                (networkSuccess, responseText) -> runOnUiThread(() -> showProfile(responseText)));
    }

    private void showProfile(String responseText) {
        thanksList.removeAllViews();
        try {
            JSONObject response = new JSONObject(responseText);
            if (!response.optBoolean("success")) {
                profileHeader.setText(response.optString("message", "Could not load profile"));
                return;
            }

            JSONObject profile = response.getJSONObject("profile");
            profileHeader.setText(profile.optString("full_name") +
                    "\nVehicle: " + profile.optString("vehicle_type"));

            JSONArray thanks = response.getJSONArray("thanks");
            if (thanks.length() == 0) {
                addThanksText("No thank-you messages yet.");
                return;
            }

            for (int i = 0; i < thanks.length(); i++) {
                JSONObject item = thanks.getJSONObject(i);
                addThanksText(item.optString("message") +
                        "\n- " + item.optString("customer_name") +
                        "\n" + item.optString("created_at"));
            }
        } catch (JSONException e) {
            profileHeader.setText(responseText);
        }
    }

    private void postThanks() {
        String message = thanksMessage.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            thanksMessage.setError("Enter a message");
            return;
        }

        postThanksButton.setEnabled(false);
        api.post("post_thanks.php",
                new FormBody.Builder()
                        .add("order_id", orderId == null ? "" : orderId)
                        .add("customer_user_id", sessionManager.getUserId())
                        .add("shopper_user_id", shopperUserId == null ? "" : shopperUserId)
                        .add("message", message),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    postThanksButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            thanksMessage.setText("");
                            loadProfile();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void addThanksText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(14, 14, 14, 14);
        thanksList.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }
}
