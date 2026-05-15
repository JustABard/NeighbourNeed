package com.example.neighbourneed;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class AdminDashboardActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private LinearLayout listLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);
        sessionManager = new SessionManager(this);
        listLayout = findViewById(R.id.admin_list);
        loadPendingShoppers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadPendingShoppers() {
        api.post("pending_shoppers.php", new FormBody.Builder(), (networkSuccess, responseText) -> runOnUiThread(() -> {
            listLayout.removeAllViews();
            try {
                JSONObject response = new JSONObject(responseText);
                if (!response.optBoolean("success")) {
                    addText(response.optString("message", "No pending shoppers."));
                    return;
                }

                JSONArray shoppers = response.getJSONArray("shoppers");
                if (shoppers.length() == 0) {
                    addText("No shoppers are waiting for approval.");
                    return;
                }

                for (int i = 0; i < shoppers.length(); i++) {
                    JSONObject shopper = shoppers.getJSONObject(i);
                    addShopper(shopper);
                }
                UiPreferences.apply(findViewById(R.id.root), sessionManager);
            } catch (JSONException e) {
                addText(responseText);
            }
        }));
    }

    private void addShopper(JSONObject shopper) {
        String userId = shopper.optString("user_id");
        TextView textView = new TextView(this);
        textView.setText(shopper.optString("full_name") + "\n" +
                shopper.optString("email") + "\nVehicle: " + shopper.optString("vehicle_type") +
                "\nID: " + shopper.optString("id_number"));
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(16, 18, 16, 8);
        listLayout.addView(textView);
        UiPreferences.apply(textView, sessionManager);

        Button approveButton = new Button(this);
        approveButton.setText("Approve Shopper");
        approveButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF6C86E));
        approveButton.setOnClickListener(view -> approveShopper(userId));
        listLayout.addView(approveButton);
        UiPreferences.apply(approveButton, sessionManager);
    }

    private void approveShopper(String userId) {
        api.post("approve_shopper.php", new FormBody.Builder().add("user_id", userId), (networkSuccess, responseText) -> runOnUiThread(() -> {
            try {
                JSONObject response = new JSONObject(responseText);
                Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                if (response.optBoolean("success")) {
                    loadPendingShoppers();
                }
            } catch (JSONException e) {
                Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void addText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(12, 12, 12, 12);
        listLayout.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }
}
