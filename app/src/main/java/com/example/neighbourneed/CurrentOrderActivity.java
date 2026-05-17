package com.example.neighbourneed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private TextView messagesTextView;
    private EditText messageEditText;
    private SessionManager sessionManager;
    private Button volunteerProfileButton;
    private Button trackButton;
    private Button cancelButton;
    private Button sendMessageButton;
    private JSONObject currentOrder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_order);

        sessionManager = new SessionManager(this);
        detailsTextView = findViewById(R.id.current_order_details);
        messagesTextView = findViewById(R.id.current_order_messages);
        messageEditText = findViewById(R.id.current_order_message_input);
        Button refreshButton = findViewById(R.id.refresh_current_order);
        volunteerProfileButton = findViewById(R.id.current_order_volunteer_profile);
        trackButton = findViewById(R.id.track_shopper);
        cancelButton = findViewById(R.id.cancel_order);
        sendMessageButton = findViewById(R.id.send_current_order_message);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadCurrentOrder();
            }
        });
        sendMessageButton.setOnClickListener(view -> sendMessage());
        cancelButton.setOnClickListener(view -> cancelOrder());
        trackButton.setOnClickListener(view -> trackShopper());

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
        messagesTextView.setText("");
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
                        trackButton.setVisibility(View.GONE);
                        cancelButton.setVisibility(View.GONE);
                        sendMessageButton.setVisibility(View.GONE);
                        messageEditText.setVisibility(View.GONE);
                        return;
                    }

                    currentOrder = jsonObject.getJSONObject("order");
                    detailsTextView.setText(formatOrder(currentOrder));
                    loadMessages();
                    String shopperUserId = currentOrder.optString("shopper_user_id");
                    if (shopperUserId == null || shopperUserId.isEmpty() || "null".equals(shopperUserId)) {
                        volunteerProfileButton.setVisibility(View.GONE);
                    } else {
                        volunteerProfileButton.setVisibility(View.VISIBLE);
                        volunteerProfileButton.setOnClickListener(view -> {
                            Intent intent = new Intent(CurrentOrderActivity.this, VolunteerProfileActivity.class);
                            intent.putExtra("shopper_user_id", shopperUserId);
                            intent.putExtra("order_id", currentOrder.optString("order_id"));
                            startActivity(intent);
                        });
                    }
                    boolean canTrack = "delivering".equals(currentOrder.optString("status"))
                            && hasCoordinate(currentOrder.optString("shopper_latitude"))
                            && hasCoordinate(currentOrder.optString("shopper_longitude"));
                    trackButton.setVisibility(canTrack ? View.VISIBLE : View.GONE);
                    cancelButton.setVisibility(canCancel(currentOrder.optString("status")) ? View.VISIBLE : View.GONE);
                    sendMessageButton.setVisibility(View.VISIBLE);
                    messageEditText.setVisibility(View.VISIBLE);
                } catch (JSONException e) {
                    detailsTextView.setText(responseText);
                    volunteerProfileButton.setVisibility(View.GONE);
                    trackButton.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadMessages() {
        if (currentOrder == null) {
            return;
        }

        api.post("order_messages.php",
                new FormBody.Builder()
                        .add("order_id", currentOrder.optString("order_id"))
                        .add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        if (!response.optBoolean("success")) {
                            messagesTextView.setText(response.optString("message", ""));
                            return;
                        }

                        org.json.JSONArray messages = response.getJSONArray("messages");
                        if (messages.length() == 0) {
                            messagesTextView.setText("No messages yet.");
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
                        messagesTextView.setText(builder.toString().trim());
                    } catch (JSONException e) {
                        messagesTextView.setText(responseText);
                    }
                }));
    }

    private void sendMessage() {
        if (currentOrder == null) {
            return;
        }
        String message = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageEditText.setError("Enter a message");
            return;
        }

        api.post("post_order_message.php",
                new FormBody.Builder()
                        .add("order_id", currentOrder.optString("order_id"))
                        .add("user_id", sessionManager.getUserId())
                        .add("message", message),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            messageEditText.setText("");
                            loadMessages();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void cancelOrder() {
        if (currentOrder == null) {
            return;
        }

        api.post("cancel_order.php",
                new FormBody.Builder()
                        .add("order_id", currentOrder.optString("order_id"))
                        .add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            loadCurrentOrder();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void trackShopper() {
        if (currentOrder == null || sessionManager.getUserId().isEmpty()) {
            return;
        }

        trackButton.setEnabled(false);
        Toast.makeText(this, "Refreshing shopper location snapshot...", Toast.LENGTH_SHORT).show();
        api.post("current_order.php",
                new FormBody.Builder().add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    trackButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        if (!response.optBoolean("success")) {
                            Toast.makeText(this, response.optString("message", "Could not refresh location"), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        currentOrder = response.getJSONObject("order");
                        detailsTextView.setText(formatOrder(currentOrder));
                        openShopperSnapshotInMaps();
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void openShopperSnapshotInMaps() {
        String latitude = currentOrder.optString("shopper_latitude");
        String longitude = currentOrder.optString("shopper_longitude");
        if (!hasCoordinate(latitude) || !hasCoordinate(longitude)) {
            Toast.makeText(this, "Shopper location is not available yet", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    private boolean hasCoordinate(String value) {
        return value != null && !value.isEmpty() && !"null".equals(value);
    }

    private boolean canCancel(String status) {
        return "pending".equals(status) || "taken".equals(status) || "shopping".equals(status);
    }

    static String formatOrder(JSONObject order) {
        String shopperName = order.optString("shopper_name");
        String shopperLine = shopperName == null || shopperName.isEmpty() || "null".equals(shopperName)
                ? ""
                : "\nVolunteer: " + shopperName +
                "\nVolunteer rating: " + order.optString("shopper_average_rating", "0") +
                " (" + order.optString("shopper_rating_count", "0") + ")" +
                formatLocationSnapshotLine(order);
        return "Order #" + order.optString("order_id") +
                "\nStatus: " + order.optString("status") +
                shopperLine +
                "\nCreated: " + order.optString("created_at") +
                "\n\nItems:\n" + order.optString("order_description") +
                "\n\nDelivery:\n" + order.optString("delivery_address") +
                "\n\nPickup:\n" + order.optString("pickup_address") +
                "\n\nNotes:\n" + order.optString("notes");
    }

    private static String formatLocationSnapshotLine(JSONObject order) {
        String updatedAt = order.optString("shopper_location_updated_at");
        if (updatedAt == null || updatedAt.isEmpty() || "null".equals(updatedAt)) {
            return "";
        }
        return "\nLocation snapshot: " + updatedAt;
    }
}
