package com.example.neighbourneed.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.R;

public class RegisterActivity extends AppCompatActivity {

    private ProgressBar loadingProgressBar;
    private Button customerButton;
    private Button shopperButton;
    private Button adminButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        loadingProgressBar = findViewById(R.id.loading);
        customerButton = findViewById(R.id.customer);
        shopperButton = findViewById(R.id.shopper);
        adminButton = findViewById(R.id.admin);

        customerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRegistration(CustomerRegisterActivity.class);
            }
        });

        shopperButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRegistration(ShopperRegisterActivity.class);
            }
        });

        adminButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openRegistration(AdminRegisterActivity.class);
            }
        });
    }

    private void openRegistration(Class<?> activityClass) {
        loadingProgressBar.setVisibility(View.GONE);
        Intent intent = new Intent(RegisterActivity.this, activityClass);
        startActivity(intent);
    }
}
