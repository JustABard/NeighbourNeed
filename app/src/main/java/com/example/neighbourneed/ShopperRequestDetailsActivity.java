package com.example.neighbourneed;

import android.content.Intent;
import android.net.Uri;
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

public class ShopperRequestDetailsActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private TextView detailTextView;
    private Button mapsButton;
    private Button takeButton;
    private Button completeButton;
    private Button profileButton;
    private JSONObject currentOrder;
    private String orderId;
    private boolean takenMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        sessionManager = new SessionManager(this);
        orderId = getIntent().getStringExtra("order_id");
        takenMode = "taken".equals(getIntent().getStringExtra("mode"));

        detailTextView = findViewById(R.id.order_detail_text);
        mapsButton = findViewById(R.id.open_maps);
        takeButton = findViewById(R.id.take_order);
        completeButton = findViewById(R.id.complete_order);
        profileButton = findViewById(R.id.view_volunteer_profile);

        takeButton.setOnClickListener(view -> takeOrder());
        completeButton.setOnClickListener(view -> completeOrder());
        mapsButton.setOnClickListener(view -> openMaps());
        profileButton.setOnClickListener(view -> openVolunteerProfile());

        loadOrder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadOrder() {
        if (orderId == null || orderId.isEmpty()) {
            detailTextView.setText("Missing order");
            return;
        }

        api.post("order_details.php", new FormBody.Builder().add("order_id", orderId), (networkSuccess, responseText) -> runOnUiThread(() -> {
            try {
                JSONObject response = new JSONObject(responseText);
                if (!response.optBoolean("success")) {
                    detailTextView.setText(response.optString("message", "Could not load order"));
                    return;
                }

                currentOrder = response.getJSONObject("order");
                renderOrder();
            } catch (JSONException e) {
                detailTextView.setText(responseText);
            }
        }));
    }

    private void renderOrder() {
        detailTextView.setText("Customer: " + currentOrder.optString("customer_name") +
                "\n\n" + CurrentOrderActivity.formatOrder(currentOrder));

        boolean isTakenByMe = sessionManager.getUserId().equals(currentOrder.optString("shopper_user_id"));
        boolean isPending = "pending".equals(currentOrder.optString("status"));
        boolean isTaken = "taken".equals(currentOrder.optString("status"));

        takeButton.setVisibility(isPending && !takenMode ? View.VISIBLE : View.GONE);
        completeButton.setVisibility(isTaken && isTakenByMe ? View.VISIBLE : View.GONE);
        profileButton.setVisibility(isTakenByMe ? View.VISIBLE : View.GONE);
    }

    private void takeOrder() {
        takeButton.setEnabled(false);
        api.post("take_order.php",
                new FormBody.Builder()
                        .add("user_id", sessionManager.getUserId())
                        .add("order_id", orderId),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    takeButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            takenMode = true;
                            loadOrder();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void completeOrder() {
        completeButton.setEnabled(false);
        api.post("complete_order.php",
                new FormBody.Builder()
                        .add("user_id", sessionManager.getUserId())
                        .add("order_id", orderId),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    completeButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            finish();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void openMaps() {
        if (currentOrder == null) {
            return;
        }

        String latitude = currentOrder.optString("delivery_latitude");
        String longitude = currentOrder.optString("delivery_longitude");
        Uri uri;
        if (latitude != null && longitude != null
                && !latitude.isEmpty() && !longitude.isEmpty()
                && !"null".equals(latitude) && !"null".equals(longitude)) {
            uri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
        } else {
            uri = Uri.parse("geo:0,0?q=" + Uri.encode(currentOrder.optString("delivery_address")));
        }

        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    private void openVolunteerProfile() {
        if (currentOrder == null) {
            return;
        }

        Intent intent = new Intent(this, VolunteerProfileActivity.class);
        intent.putExtra("shopper_user_id", sessionManager.getUserId());
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }
}
