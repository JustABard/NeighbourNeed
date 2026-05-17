package com.example.neighbourneed.ui.login;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.R;

import okhttp3.FormBody;

public class CustomerRegisterActivity extends AppCompatActivity {

    private final RegistrationClient registrationClient = new RegistrationClient();
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_customer);

        fullNameEditText = findViewById(R.id.customer_full_name);
        emailEditText = findViewById(R.id.customer_email);
        passwordEditText = findViewById(R.id.customer_password);
        ImageButton togglePasswordButton = findViewById(R.id.customer_toggle_password_visibility);
        registerButton = findViewById(R.id.register_customer);
        PasswordVisibilityToggle.attach(passwordEditText, togglePasswordButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerCustomer();
            }
        });
    }

    private void registerCustomer() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();

        if (!validateCommonFields(fullName, email, password)) {
            return;
        }

        registerButton.setEnabled(false);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_type", "customer")
                .add("full_name", fullName)
                .add("email", email)
                .add("password", password);

        registrationClient.register(formBuilder, this::showResult);
    }

    private boolean validateCommonFields(String fullName, String email, String password) {
        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Enter your full name");
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            return false;
        }

        if (password.trim().length() <= 5) {
            passwordEditText.setError("Password must be >5 characters");
            return false;
        }

        return true;
    }

    private void showResult(boolean success, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                registerButton.setEnabled(true);
                if (success) {
                    showReturnToLoginDialog(message);
                } else {
                    Toast.makeText(CustomerRegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showReturnToLoginDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Registration successful")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Return to login", (dialogInterface, i) -> {
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }
}
