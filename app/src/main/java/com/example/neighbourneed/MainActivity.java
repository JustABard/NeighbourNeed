package com.example.neighbourneed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.neighbourneed.data.SessionManager;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        setTitle(R.string.customer_dashboard_title);
        sessionManager = new SessionManager(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button placeOrderButton = findViewById(R.id.place_order);
        Button currentOrderButton = findViewById(R.id.current_order);
        Button orderHistoryButton = findViewById(R.id.order_history);
        Button accountSettingsButton = findViewById(R.id.account_settings);
        Button logoutButton = findViewById(R.id.logout);
        View supportButton = findViewById(R.id.support_button);

        placeOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, PlaceOrderActivity.class));
            }
        });

        currentOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CurrentOrderActivity.class));
            }
        });

        orderHistoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, OrderHistoryActivity.class));
            }
        });

        accountSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, AccountSettingsActivity.class));
            }
        });

        logoutButton.setOnClickListener(view -> LogoutHelper.logout(this, sessionManager));
        supportButton.setOnClickListener(view -> startActivity(new Intent(this, SupportTicketsActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.main), sessionManager);
    }
}
