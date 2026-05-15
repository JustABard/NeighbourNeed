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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_order);

        sessionManager = new SessionManager(this);
        detailsTextView = findViewById(R.id.current_order_details);
        Button refreshButton = findViewById(R.id.refresh_current_order);

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loadCurrentOrder();
            }
        });

        loadCurrentOrder();
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
                        return;
                    }

                    JSONObject order = jsonObject.getJSONObject("order");
                    detailsTextView.setText(formatOrder(order));
                } catch (JSONException e) {
                    detailsTextView.setText(responseText);
                }
            }
        });
    }

    static String formatOrder(JSONObject order) {
        return "Order #" + order.optString("order_id") +
                "\nStatus: " + order.optString("status") +
                "\nCreated: " + order.optString("created_at") +
                "\n\nItems:\n" + order.optString("order_description") +
                "\n\nDelivery:\n" + order.optString("delivery_address") +
                "\n\nPickup:\n" + order.optString("pickup_address") +
                "\n\nNotes:\n" + order.optString("notes");
    }
}
