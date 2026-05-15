package com.example.neighbourneed.ui.login;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.R;

import okhttp3.FormBody;

public class AdminRegisterActivity extends AppCompatActivity {

    private final RegistrationClient registrationClient = new RegistrationClient();
    private EditText fullNameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText employeeIdEditText;
    private RadioGroup roleGroup;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_admin);

        fullNameEditText = findViewById(R.id.admin_full_name);
        emailEditText = findViewById(R.id.admin_email);
        passwordEditText = findViewById(R.id.admin_password);
        employeeIdEditText = findViewById(R.id.admin_employee_id);
        roleGroup = findViewById(R.id.admin_role_group);
        registerButton = findViewById(R.id.register_admin);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerAdmin();
            }
        });
    }

    private void registerAdmin() {
        String fullName = fullNameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String employeeId = employeeIdEditText.getText().toString().trim();
        int selectedRoleId = roleGroup.getCheckedRadioButtonId();

        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Enter your full name");
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

        if (TextUtils.isEmpty(employeeId)) {
            employeeIdEditText.setError("Enter your employee ID");
            return;
        }

        if (selectedRoleId == -1) {
            Toast.makeText(this, "Select an internal role", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedRole = findViewById(selectedRoleId);
        String adminRole = selectedRole.getText().toString();

        registerButton.setEnabled(false);

        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_type", "admin")
                .add("full_name", fullName)
                .add("email", email)
                .add("password", password)
                .add("employee_id", employeeId)
                .add("admin_role", adminRole);

        registrationClient.register(formBuilder, this::showResult);
    }

    private void showResult(boolean success, String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                registerButton.setEnabled(true);
                Toast.makeText(AdminRegisterActivity.this, message, Toast.LENGTH_SHORT).show();
                if (success) {
                    finish();
                }
            }
        });
    }
}
