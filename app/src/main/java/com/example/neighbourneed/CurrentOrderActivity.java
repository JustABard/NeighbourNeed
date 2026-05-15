package com.example.neighbourneed;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class CurrentOrderActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private TextView detailsTextView;
    private SessionManager sessionManager;
    private Button volunteerProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_order);

        sessionManager = new SessionManager(this);
        detailsTextView = findViewById(R.id.current_order_details);
        Button refreshButton = findViewById(R.id.refresh_current_order);
        volunteerProfileButton = findViewById(R.id.current_order_volunteer_profile);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadCurrentOrder();
            }
        });

        loadCurrentOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadCurrentOrder() {
        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        detailsTextView.setText("Loading...");
        api.post("current_order.php", new FormBody.Builder().add("user_id", userId), this::showOrder);
    }

    private void showOrder(boolean networkSuccess, String responseText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject(responseText);
                    if (!jsonObject.optBoolean("success")) {
                        detailsTextView.setText(jsonObject.optString("message", "No current order"));
                        volunteerProfileButton.setVisibility(View.GONE);
                        return;
                    }

                    JSONObject order = jsonObject.getJSONObject("order");
                    detailsTextView.setText(formatOrder(order));
                    String shopperUserId = order.optString("shopper_user_id");
                    if (shopperUserId == null || shopperUserId.isEmpty() || "null".equals(shopperUserId)) {
                        volunteerProfileButton.setVisibility(View.GONE);
                    } else {
                        volunteerProfileButton.setVisibility(View.VISIBLE);
                        volunteerProfileButton.setOnClickListener(view -> {
                            android.content.Intent intent = new android.content.Intent(CurrentOrderActivity.this, VolunteerProfileActivity.class);
                            intent.putExtra("shopper_user_id", shopperUserId);
                            intent.putExtra("order_id", order.optString("order_id"));
                            startActivity(intent);
                        });
                    }
                } catch (JSONException e) {
                    detailsTextView.setText(responseText);
                    volunteerProfileButton.setVisibility(View.GONE);
                }
            }
        });
    }

    static String formatOrder(JSONObject order) {
        String shopperName = order.optString("shopper_name");
        String shopperLine = shopperName == null || shopperName.isEmpty() || "null".equals(shopperName)
                ? ""
                : "\nVolunteer: " + shopperName;
        return "Order #" + order.optString("order_id") +
                "\nStatus: " + order.optString("status") +
                shopperLine +
                "\nCreated: " + order.optString("created_at") +
                "\n\nItems:\n" + order.optString("order_description") +
                "\n\nDelivery:\n" + order.optString("delivery_address") +
                "\n\nPickup:\n" + order.optString("pickup_address") +
                "\n\nNotes:\n" + order.optString("notes");
    }
}
