package com.example.neighbourneed;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class ShopperRequestsActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private LinearLayout listLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);
        sessionManager = new SessionManager(this);
        ((TextView) findViewById(R.id.list_title)).setText("Open Requests");
        listLayout = findViewById(R.id.list_container);
        loadRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadRequests() {
        api.post("shopper_requests.php", new FormBody.Builder(), (networkSuccess, responseText) -> runOnUiThread(() -> {
            listLayout.removeAllViews();
            try {
                JSONObject response = new JSONObject(responseText);
                if (!response.optBoolean("success")) {
                    addText(response.optString("message", "No open requests."));
                    return;
                }

                JSONArray orders = response.getJSONArray("orders");
                if (orders.length() == 0) {
                    addText("No open requests right now.");
                    return;
                }

                for (int i = 0; i < orders.length(); i++) {
                    addOrder(orders.getJSONObject(i));
                }
                UiPreferences.apply(findViewById(R.id.root), sessionManager);
            } catch (JSONException e) {
                addText(responseText);
            }
        }));
    }

    private void addOrder(JSONObject order) {
        CardView cardView = new CardView(this);
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setRadius(dp(14));
        cardView.setCardElevation(dp(4));
        cardView.setUseCompatPadding(true);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, dp(14));

        TextView textView = new TextView(this);
        textView.setText("Order #" + order.optString("order_id") +
                "\nCustomer: " + order.optString("customer_name") +
                "\nDelivery: " + order.optString("delivery_address") +
                "\nStatus: " + order.optString("status") +
                "\n\nTap to view and accept");
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(dp(18), dp(16), dp(18), dp(16));

        cardView.setOnClickListener(view -> {
            Intent intent = new Intent(this, ShopperRequestDetailsActivity.class);
            intent.putExtra("order_id", order.optString("order_id"));
            startActivity(intent);
        });
        cardView.addView(textView);
        listLayout.addView(cardView, cardParams);
        UiPreferences.apply(textView, sessionManager);
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

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
