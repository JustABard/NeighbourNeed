package com.example.neighbourneed;

import android.graphics.Typeface;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.FormBody;

public class PlaceOrderActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private final List<OrderItem> orderItems = new ArrayList<>();

    private EditText itemNameEditText;
    private EditText itemPriceEstimateEditText;
    private EditText itemQuantityEditText;
    private EditText preferredStoreEditText;
    private EditText deliveryAddressEditText;
    private EditText notesEditText;
    private LinearLayout itemListLayout;
    private Button submitButton;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_order);

        sessionManager = new SessionManager(this);
        itemNameEditText = findViewById(R.id.item_name);
        itemPriceEstimateEditText = findViewById(R.id.item_price_estimate);
        itemQuantityEditText = findViewById(R.id.item_quantity);
        preferredStoreEditText = findViewById(R.id.preferred_store);
        deliveryAddressEditText = findViewById(R.id.delivery_address);
        notesEditText = findViewById(R.id.order_notes);
        itemListLayout = findViewById(R.id.item_list);
        Button addItemButton = findViewById(R.id.add_item);
        submitButton = findViewById(R.id.submit_order);

        String defaultLocation = sessionManager.getDefaultLocation();
        if (!TextUtils.isEmpty(defaultLocation)) {
            deliveryAddressEditText.setText(defaultLocation);
        }

        addItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitOrder();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void addItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String priceEstimate = itemPriceEstimateEditText.getText().toString().trim();
        String quantityText = itemQuantityEditText.getText().toString().trim();

        if (TextUtils.isEmpty(itemName)) {
            itemNameEditText.setError("Enter an item name");
            return;
        }

        if (TextUtils.isEmpty(quantityText)) {
            itemQuantityEditText.setError("Enter a quantity");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
        } catch (NumberFormatException e) {
            itemQuantityEditText.setError("Quantity must be a number");
            return;
        }

        if (quantity <= 0) {
            itemQuantityEditText.setError("Quantity must be greater than 0");
            return;
        }

        orderItems.add(new OrderItem(itemName, priceEstimate, quantity));
        itemNameEditText.setText("");
        itemPriceEstimateEditText.setText("");
        itemQuantityEditText.setText("");
        renderItems();
    }

    private void renderItems() {
        itemListLayout.removeAllViews();

        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem item = orderItems.get(i);
            TextView textView = new TextView(this);
            textView.setText((i + 1) + ". " + item.toDisplayText());
            textView.setTextColor(0xFF102A3A);
            textView.setTextSize(15);
            textView.setPadding(12, 10, 12, 10);
            if (sessionManager.isBoldTextEnabled()) {
                textView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            }
            itemListLayout.addView(textView);
            UiPreferences.apply(textView, sessionManager);
        }
    }

    private void submitOrder() {
        String userId = sessionManager.getUserId();
        String preferredStore = preferredStoreEditText.getText().toString().trim();
        String deliveryAddress = deliveryAddressEditText.getText().toString().trim();
        String notes = notesEditText.getText().toString().trim();

        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        if (orderItems.isEmpty()) {
            Toast.makeText(this, "Add at least one item", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(deliveryAddress)) {
            deliveryAddressEditText.setError("Enter a delivery location");
            return;
        }

        submitButton.setEnabled(false);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_id", userId)
                .add("order_description", buildOrderDescription())
                .add("pickup_address", preferredStore)
                .add("delivery_address", deliveryAddress)
                .add("delivery_latitude", sessionManager.getDefaultLatitude())
                .add("delivery_longitude", sessionManager.getDefaultLongitude())
                .add("notes", notes);

        api.post("create_order.php", formBuilder, this::handleCreateOrderResponse);
    }

    private String buildOrderDescription() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < orderItems.size(); i++) {
            builder.append(i + 1)
                    .append(". ")
                    .append(orderItems.get(i).toDisplayText());
            if (i < orderItems.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    private void handleCreateOrderResponse(boolean networkSuccess, String responseText) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                submitButton.setEnabled(true);
                try {
                    JSONObject jsonObject = new JSONObject(responseText);
                    boolean success = jsonObject.optBoolean("success");
                    String message = jsonObject.optString("message", "Could not create order");
                    Toast.makeText(PlaceOrderActivity.this, message, Toast.LENGTH_SHORT).show();
                    if (success) {
                        finish();
                    }
                } catch (JSONException e) {
                    Toast.makeText(PlaceOrderActivity.this, responseText, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private static class OrderItem {
        private final String name;
        private final String priceEstimate;
        private final int quantity;

        OrderItem(String name, String priceEstimate, int quantity) {
            this.name = name;
            this.priceEstimate = priceEstimate;
            this.quantity = quantity;
        }

        String toDisplayText() {
            String priceText = TextUtils.isEmpty(priceEstimate)
                    ? "No price estimate"
                    : "Estimated price: R" + priceEstimate + " each";
            return name + "\n" + priceText + "\nQuantity: " + quantity;
        }
    }
}
