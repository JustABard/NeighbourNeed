package com.example.neighbourneed.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.R;

import okhttp3.FormBody;

public class ShopperRegisterActivity extends AppCompatActivity {

    private final RegistrationClient registrationClient = new RegistrationClient();
    private EditText fullNameEditText;
    private EditText idNumberEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Spinner vehicleSpinner;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_shopper);

        fullNameEditText = findViewById(R.id.shopper_full_name);
        idNumberEditText = findViewById(R.id.shopper_id_number);
        emailEditText = findViewById(R.id.shopper_email);
        passwordEditText = findViewById(R.id.shopper_password);
        vehicleSpinner = findViewById(R.id.vehicle_spinner);
        registerButton = findViewById(R.id.register_shopper);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Car", "Motorbike", "Bicycle", "Walking"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerShopper();
            }
        });
    }

    private void registerShopper() {
        String fullName = fullNameEditText.getText().toString().trim();
        String idNumber = idNumberEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String vehicleType = vehicleSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Enter your full name");
            return;
        }

        if (TextUtils.isEmpty(idNumber)) {
            idNumberEditText.setError("Enter your ID number");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return;
        }

        if (password.trim().length() <= 5) {
            passwordEditText.setError("Password must be >5 characters");
            return;
        }

        registerButton.setEnabled(false);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_type", "shopper")
                .add("full_name", fullName)
                .add("email", email)
                .add("password", password)
                .add("id_number", idNumber)
                .add("vehicle_type", vehicleType);

        registrationClient.register(formBuilder, this::showResult);
    }

    private void showResult(boolean success, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                registerButton.setEnabled(true);
                Toast.makeText(ShopperRegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                if (success) {
                    finish();
                }
            }
        });
    }
}
