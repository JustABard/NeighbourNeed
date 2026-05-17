package com.example.neighbourneed;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

public class ShopperDashboardActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopper_dashboard);

        sessionManager = new SessionManager(this);
        TextView statusTextView = findViewById(R.id.shopper_status);
        statusTextView.setText("Shopper Home\nApproval required before taking orders");

        Button viewRequestsButton = findViewById(R.id.view_requests);
        Button viewTakenOrderButton = findViewById(R.id.view_taken_order);
        Button settingsButton = findViewById(R.id.shopper_account_settings);
        Button profileButton = findViewById(R.id.shopper_profile);
        Button logoutButton = findViewById(R.id.logout);
        android.view.View supportButton = findViewById(R.id.support_button);

        viewRequestsButton.setOnClickListener(view -> startActivity(new Intent(this, ShopperRequestsActivity.class)));
        viewTakenOrderButton.setOnClickListener(view -> startActivity(new Intent(this, ShopperCurrentOrderActivity.class)));
        settingsButton.setOnClickListener(view -> startActivity(new Intent(this, AccountSettingsActivity.class)));
        profileButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, VolunteerProfileActivity.class);
            intent.putExtra("shopper_user_id", sessionManager.getUserId());
            startActivity(intent);
        });
        logoutButton.setOnClickListener(view -> LogoutHelper.logout(this, sessionManager));
        supportButton.setOnClickListener(view -> startActivity(new Intent(this, SupportTicketsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }
}
