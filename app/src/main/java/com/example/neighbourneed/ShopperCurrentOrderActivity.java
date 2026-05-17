package com.example.neighbourneed;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class ShopperCurrentOrderActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private LinearLayout listLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);
        sessionManager = new SessionManager(this);
        ((TextView) findViewById(R.id.list_title)).setText("My Taken Order");
        listLayout = findViewById(R.id.list_container);
        loadTakenOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadTakenOrder() {
        listLayout.removeAllViews();
        api.post("shopper_current_order.php", new FormBody.Builder().add("user_id", sessionManager.getUserId()), (networkSuccess, responseText) -> runOnUiThread(() -> {
            try {
                JSONObject response = new JSONObject(responseText);
                if (!response.optBoolean("success")) {
                    addText(response.optString("message", "No taken order."));
                    return;
                }
                JSONObject order = response.getJSONObject("order");
                addText(CurrentOrderActivity.formatOrder(order) + "\n\nTap to open details.");
                listLayout.getChildAt(0).setOnClickListener(view -> {
                    Intent intent = new Intent(this, ShopperRequestDetailsActivity.class);
                    intent.putExtra("order_id", order.optString("order_id"));
                    intent.putExtra("mode", "taken");
                    startActivity(intent);
                });
                loadMessages(order.optString("order_id"));
                UiPreferences.apply(findViewById(R.id.root), sessionManager);
            } catch (JSONException e) {
                addText(responseText);
            }
        }));
    }

    private void loadMessages(String orderId) {
        api.post("order_messages.php",
                new FormBody.Builder()
                        .add("order_id", orderId)
                        .add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        if (!response.optBoolean("success")) {
                            addText(response.optString("message", ""));
                            return;
                        }

                        JSONArray messages = response.getJSONArray("messages");
                        if (messages.length() == 0) {
                            addText("Messages:\nNo messages yet.");
                            return;
                        }

                        StringBuilder builder = new StringBuilder("Messages:\n");
                        for (int i = 0; i < messages.length(); i++) {
                            JSONObject message = messages.getJSONObject(i);
                            builder.append(message.optString("sender_name"))
                                    .append(": ")
                                    .append(message.optString("message"))
                                    .append("\n");
                        }
                        builder.append("\nTap the order details to reply.");
                        addText(builder.toString().trim());
                    } catch (JSONException e) {
                        addText(responseText);
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
