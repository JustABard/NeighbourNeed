package com.example.neighbourneed;

import android.content.Intent;
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

public class OrderHistoryActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private LinearLayout container;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        sessionManager = new SessionManager(this);
        container = findViewById(R.id.order_history_container);
        loadHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadHistory() {
        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        api.post("order_history.php", new FormBody.Builder().add("user_id", userId), this::showHistory);
    }

    private void showHistory(boolean networkSuccess, String responseText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(responseText);
                    if (!jsonObject.optBoolean("success")) {
                        addHistoryText(jsonObject.optString("message", "No order history"));
                        return;
                    }

                    JSONArray orders = jsonObject.getJSONArray("orders");
                    if (orders.length() == 0) {
                        addHistoryText("No completed orders yet.");
                        return;
                    }

                    for (int i = 0; i < orders.length(); i++) {
                        addOrder(orders.getJSONObject(i));
                    }
                } catch (JSONException e) {
                    addHistoryText(responseText);
                }
            }
        });
    }

    private void addOrder(JSONObject order) {
        addHistoryText(CurrentOrderActivity.formatOrder(order));

        String shopperUserId = order.optString("shopper_user_id");
        if (shopperUserId == null || shopperUserId.isEmpty() || "null".equals(shopperUserId)) {
            return;
        }

        Button thanksButton = new Button(this);
        thanksButton.setText("Thank Volunteer");
        thanksButton.setTextColor(0xFF071B25);
        thanksButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD6C7FF));
        thanksButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, VolunteerProfileActivity.class);
            intent.putExtra("shopper_user_id", shopperUserId);
            intent.putExtra("order_id", order.optString("order_id"));
            startActivity(intent);
        });
        container.addView(thanksButton);
        UiPreferences.apply(thanksButton, sessionManager);
    }

    private void addHistoryText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(20, 20, 20, 20);
        container.addView(textView);
    }
}
