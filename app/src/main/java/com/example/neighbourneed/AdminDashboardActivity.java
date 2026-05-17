package com.example.neighbourneed;

import android.content.res.ColorStateList;
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

public class AdminDashboardActivity extends AppCompatActivity {

    private static final int COLOR_TEXT = 0xFF102A3A;
    private static final int COLOR_WARNING = 0xFFE57373;
    private static final int COLOR_OK = 0xFF71D7C7;
    private static final int COLOR_SECONDARY = 0xFFD6C7FF;

    private final CustomerApi api = new CustomerApi();
    private LinearLayout listLayout;
    private SessionManager sessionManager;
    private TextView titleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        sessionManager = new SessionManager(this);
        titleTextView = findViewById(R.id.admin_title);
        listLayout = findViewById(R.id.admin_list);

        Button pendingShoppersButton = findViewById(R.id.view_pending_shoppers);
        Button customersButton = findViewById(R.id.view_customers);
        Button shoppersButton = findViewById(R.id.view_shoppers);
        Button adminsButton = findViewById(R.id.view_admins);
        Button supportTicketsButton = findViewById(R.id.view_support_tickets);
        Button logoutButton = findViewById(R.id.logout);

        pendingShoppersButton.setOnClickListener(view -> loadPendingShoppers());
        customersButton.setOnClickListener(view -> loadAccounts("customers", "Current Customers"));
        shoppersButton.setOnClickListener(view -> loadAccounts("shoppers", "Current Shoppers"));
        adminsButton.setOnClickListener(view -> loadAccounts("admins", "Current Admins"));
        supportTicketsButton.setOnClickListener(view -> startActivity(new Intent(this, SupportTicketsActivity.class)));
        logoutButton.setOnClickListener(view -> LogoutHelper.logout(this, sessionManager));

        loadPendingShoppers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadPendingShoppers() {
        titleTextView.setText("Pending Shoppers");
        listLayout.removeAllViews();
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
                    addPendingShopper(shopper);
                }
                UiPreferences.apply(findViewById(R.id.root), sessionManager);
            } catch (JSONException e) {
                addText(responseText);
            }
        }));
    }

    private void loadAccounts(String accountType, String title) {
        titleTextView.setText(title);
        listLayout.removeAllViews();
        addText("Loading...");

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("actor_user_id", sessionManager.getUserId())
                .add("account_type", accountType);

        api.post("admin_accounts.php", formBuilder, (networkSuccess, responseText) -> runOnUiThread(() -> {
            listLayout.removeAllViews();
            try {
                JSONObject response = new JSONObject(responseText);
                if (!response.optBoolean("success")) {
                    addText(response.optString("message", "Could not load accounts"));
                    return;
                }

                JSONArray accounts = response.getJSONArray("accounts");
                if (accounts.length() == 0) {
                    addText("No accounts found.");
                    return;
                }

                for (int i = 0; i < accounts.length(); i++) {
                    JSONObject account = accounts.getJSONObject(i);
                    if ("customers".equals(accountType)) {
                        addCustomer(account);
                    } else if ("shoppers".equals(accountType)) {
                        addShopper(account);
                    } else {
                        addAdmin(account);
                    }
                }
                UiPreferences.apply(findViewById(R.id.root), sessionManager);
            } catch (JSONException e) {
                addText(responseText);
            }
        }));
    }

    private void addPendingShopper(JSONObject shopper) {
        String userId = shopper.optString("user_id");
        addAccountText(shopper.optString("full_name") + "\n" +
                shopper.optString("email") + "\nVehicle: " + shopper.optString("vehicle_type") +
                "\nID: " + shopper.optString("id_number"));
        addActionButton("Approve Shopper", COLOR_OK, () -> approveShopper(userId));
    }

    private void addCustomer(JSONObject customer) {
        String userId = customer.optString("user_id");
        boolean suspended = isTrue(customer.opt("suspended"));
        addAccountText(customer.optString("full_name") + "\n" +
                customer.optString("email") + "\nStatus: " + (suspended ? "Suspended" : "Active"));

        if (suspended) {
            addActionButton("Unsuspend Customer", COLOR_OK, () -> runAccountAction("unsuspend_customer", userId, "customers"));
        } else {
            addActionButton("Suspend Customer", COLOR_WARNING, () -> runAccountAction("suspend_customer", userId, "customers"));
        }
    }

    private void addShopper(JSONObject shopper) {
        String userId = shopper.optString("user_id");
        boolean approved = isTrue(shopper.opt("approved"));
        addAccountText(shopper.optString("full_name") + "\n" +
                shopper.optString("email") + "\nVehicle: " + shopper.optString("vehicle_type") +
                "\nStatus: " + (approved ? "Verified" : "Not verified"));

        if (approved) {
            addActionButton("Unverify Shopper", COLOR_WARNING, () -> runAccountAction("unverify_shopper", userId, "shoppers"));
        } else {
            addActionButton("Verify Shopper", COLOR_OK, () -> runAccountAction("verify_shopper", userId, "shoppers"));
        }
    }

    private void addAdmin(JSONObject admin) {
        String userId = admin.optString("user_id");
        boolean verified = isTrue(admin.opt("verified"));
        addAccountText(admin.optString("full_name") + "\n" +
                admin.optString("email") + "\nRole: " + admin.optString("admin_role") +
                "\nEmployee ID: " + admin.optString("employee_id") +
                "\nStatus: " + (verified ? "Verified" : "Not verified"));

        if (sessionManager.getUserId().equals(userId)) {
            addText("You cannot change your own admin verification from here.");
            return;
        }

        if (verified) {
            addActionButton("Unverify Admin", COLOR_WARNING, () -> runAccountAction("unverify_admin", userId, "admins"));
        } else {
            addActionButton("Verify Admin", COLOR_SECONDARY, () -> runAccountAction("verify_admin", userId, "admins"));
        }
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

    private void runAccountAction(String action, String targetUserId, String currentList) {
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("actor_user_id", sessionManager.getUserId())
                .add("target_user_id", targetUserId)
                .add("action", action);

        api.post("admin_account_action.php", formBuilder, (networkSuccess, responseText) -> runOnUiThread(() -> {
            try {
                JSONObject response = new JSONObject(responseText);
                Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                if (response.optBoolean("success")) {
                    if ("customers".equals(currentList)) {
                        loadAccounts("customers", "Current Customers");
                    } else if ("shoppers".equals(currentList)) {
                        loadAccounts("shoppers", "Current Shoppers");
                    } else {
                        loadAccounts("admins", "Current Admins");
                    }
                }
            } catch (JSONException e) {
                Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
            }
        }));
    }

    private void addAccountText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(COLOR_TEXT);
        textView.setTextSize(16);
        textView.setPadding(16, 18, 16, 8);
        listLayout.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }

    private void addActionButton(String text, int color, Runnable action) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(0xFF071B25);
        button.setBackgroundTintList(ColorStateList.valueOf(color));
        button.setOnClickListener(view -> action.run());
        listLayout.addView(button);
        UiPreferences.apply(button, sessionManager);
    }

    private void addText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(COLOR_TEXT);
        textView.setTextSize(16);
        textView.setPadding(12, 12, 12, 12);
        listLayout.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value == null) {
            return false;
        }
        String text = String.valueOf(value).trim().toLowerCase();
        return "true".equals(text) || "t".equals(text) || "1".equals(text);
    }
}
