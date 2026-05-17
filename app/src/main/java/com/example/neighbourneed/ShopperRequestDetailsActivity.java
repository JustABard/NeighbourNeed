package com.example.neighbourneed;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class ShopperRequestDetailsActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private TextView detailTextView;
    private TextView messagesTextView;
    private Button mapsButton;
    private Button takeButton;
    private Button nextStageButton;
    private Button updateLocationButton;
    private Button profileButton;
    private Button releaseButton;
    private Button sendMessageButton;
    private EditText messageEditText;
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
        messagesTextView = findViewById(R.id.order_detail_messages);
        mapsButton = findViewById(R.id.open_maps);
        takeButton = findViewById(R.id.take_order);
        nextStageButton = findViewById(R.id.complete_order);
        updateLocationButton = findViewById(R.id.update_location_snapshot);
        profileButton = findViewById(R.id.view_volunteer_profile);
        releaseButton = findViewById(R.id.release_order);
        sendMessageButton = findViewById(R.id.send_order_message);
        messageEditText = findViewById(R.id.order_message_input);

        takeButton.setOnClickListener(view -> takeOrder());
        nextStageButton.setOnClickListener(view -> updateStage());
        updateLocationButton.setOnClickListener(view -> updateLocationSnapshot());
        mapsButton.setOnClickListener(view -> openMaps());
        profileButton.setOnClickListener(view -> openVolunteerProfile());
        sendMessageButton.setOnClickListener(view -> sendMessage());
        releaseButton.setOnClickListener(view -> releaseOrder());

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

        takeButton.setVisibility(isPending && !takenMode ? View.VISIBLE : View.GONE);
        nextStageButton.setVisibility(isTakenByMe && getNextStage() != null ? View.VISIBLE : View.GONE);
        nextStageButton.setText(getNextStageLabel());
        updateLocationButton.setVisibility(isTakenByMe && "delivering".equals(currentOrder.optString("status")) ? View.VISIBLE : View.GONE);
        profileButton.setVisibility(View.GONE);
        sendMessageButton.setVisibility(isTakenByMe ? View.VISIBLE : View.GONE);
        messageEditText.setVisibility(isTakenByMe ? View.VISIBLE : View.GONE);
        releaseButton.setVisibility(isTakenByMe && canRelease() ? View.VISIBLE : View.GONE);

        if (isTakenByMe) {
            loadMessages();
        } else {
            messagesTextView.setText("");
        }
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

    private void updateStage() {
        String nextStage = getNextStage();
        if (nextStage == null) {
            return;
        }

        nextStageButton.setEnabled(false);
        if ("delivering".equals(nextStage)) {
            Toast.makeText(this, "Taking current location snapshot...", Toast.LENGTH_SHORT).show();
            requestCurrentLocation(location -> postStageUpdate(nextStage, location));
        } else {
            postStageUpdate(nextStage, null);
        }
    }

    private void postStageUpdate(String nextStage, Location location) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_id", sessionManager.getUserId())
                .add("order_id", orderId)
                .add("stage", nextStage);

        if (location != null) {
            formBuilder.add("shopper_latitude", String.valueOf(location.getLatitude()));
            formBuilder.add("shopper_longitude", String.valueOf(location.getLongitude()));
        }

        api.post("update_order_stage.php",
                formBuilder,
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    nextStageButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            loadOrder();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void updateLocationSnapshot() {
        if (currentOrder == null || !"delivering".equals(currentOrder.optString("status"))) {
            Toast.makeText(this, "Location snapshots are only available while delivering", Toast.LENGTH_SHORT).show();
            return;
        }

        updateLocationButton.setEnabled(false);
        Toast.makeText(this, "Taking current location snapshot...", Toast.LENGTH_SHORT).show();
        requestCurrentLocation(location -> {
            if (location == null) {
                updateLocationButton.setEnabled(true);
                Toast.makeText(this, "Could not get current location", Toast.LENGTH_SHORT).show();
                return;
            }

            api.post("update_shopper_location.php",
                    new FormBody.Builder()
                            .add("user_id", sessionManager.getUserId())
                            .add("order_id", orderId)
                            .add("shopper_latitude", String.valueOf(location.getLatitude()))
                            .add("shopper_longitude", String.valueOf(location.getLongitude())),
                    (networkSuccess, responseText) -> runOnUiThread(() -> {
                        updateLocationButton.setEnabled(true);
                        try {
                            JSONObject response = new JSONObject(responseText);
                            Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                            if (response.optBoolean("success")) {
                                loadOrder();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                        }
                    }));
        });
    }

    private void sendMessage() {
        String message = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageEditText.setError("Enter a message");
            return;
        }

        api.post("post_order_message.php",
                new FormBody.Builder()
                        .add("user_id", sessionManager.getUserId())
                        .add("order_id", orderId)
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

                        JSONArray messages = response.getJSONArray("messages");
                        if (messages.length() == 0) {
                            messagesTextView.setText("Messages:\nNo messages yet.");
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

    private void releaseOrder() {
        api.post("release_order.php",
                new FormBody.Builder()
                        .add("user_id", sessionManager.getUserId())
                        .add("order_id", orderId),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
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

    private String getNextStage() {
        if (currentOrder == null) {
            return null;
        }
        String status = currentOrder.optString("status");
        if ("taken".equals(status)) {
            return "shopping";
        }
        if ("shopping".equals(status)) {
            return "delivering";
        }
        if ("delivering".equals(status)) {
            return "completed";
        }
        return null;
    }

    private boolean canRelease() {
        if (currentOrder == null) {
            return false;
        }
        String status = currentOrder.optString("status");
        return "taken".equals(status) || "shopping".equals(status);
    }

    private String getNextStageLabel() {
        String stage = getNextStage();
        if ("shopping".equals(stage)) {
            return "Start Shopping";
        }
        if ("delivering".equals(stage)) {
            return "Start Delivering";
        }
        if ("completed".equals(stage)) {
            return "Mark Completed";
        }
        return "Next Stage";
    }

    private Location getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
            return null;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return null;
        }

        Location bestLocation = null;
        for (String provider : locationManager.getProviders(true)) {
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null && (bestLocation == null || location.getAccuracy() < bestLocation.getAccuracy())) {
                bestLocation = location;
            }
        }
        return bestLocation;
    }

    private void requestCurrentLocation(LocationCallback callback) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 44);
            callback.onLocation(null);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            callback.onLocation(null);
            return;
        }

        String provider = null;
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        if (provider == null) {
            callback.onLocation(getLastKnownLocation());
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(provider, null, getMainExecutor(),
                    location -> callback.onLocation(location != null ? location : getLastKnownLocation()));
            return;
        }

        final boolean[] completed = {false};
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (completed[0]) {
                    return;
                }
                completed[0] = true;
                locationManager.removeUpdates(this);
                callback.onLocation(location);
            }
        };

        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (completed[0]) {
                return;
            }
            completed[0] = true;
            locationManager.removeUpdates(listener);
            callback.onLocation(getLastKnownLocation());
        }, 8000);
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

    private interface LocationCallback {
        void onLocation(Location location);
    }
}
