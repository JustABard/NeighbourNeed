package com.example.neighbourneed;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;
import com.example.neighbourneed.ui.login.LoginActivity;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class AccountSettingsActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private EditText fullNameEditText;
    private EditText defaultLocationEditText;
    private CheckBox boldTextCheckBox;
    private TextView emailTextView;
    private TextView userTypeTextView;
    private Button saveButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_settings);

        sessionManager = new SessionManager(this);
        fullNameEditText = findViewById(R.id.account_full_name);
        defaultLocationEditText = findViewById(R.id.account_default_location);
        boldTextCheckBox = findViewById(R.id.bold_text_setting);
        emailTextView = findViewById(R.id.account_email);
        userTypeTextView = findViewById(R.id.account_user_type);
        saveButton = findViewById(R.id.save_account);
        deleteButton = findViewById(R.id.delete_account);

        boldTextCheckBox.setChecked(sessionManager.isBoldTextEnabled());
        defaultLocationEditText.setText(sessionManager.getDefaultLocation());
        applyBoldTextPreference();

        saveButton.setOnClickListener(view -> saveAccount());
        deleteButton.setOnClickListener(view -> confirmDeleteAccount());
        loadAccount();
    }

    private void loadAccount() {
        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            Toast.makeText(this, "Please log in again", Toast.LENGTH_SHORT).show();
            return;
        }

        api.post("account_details.php", new FormBody.Builder().add("user_id", userId), this::showAccount);
    }

    private void showAccount(boolean networkSuccess, String responseText) {
        runOnUiThread(() -> {
            try {
                JSONObject jsonObject = new JSONObject(responseText);
                if (!jsonObject.optBoolean("success")) {
                    Toast.makeText(this, jsonObject.optString("message", "Could not load account"), Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject user = jsonObject.getJSONObject("user");
                fullNameEditText.setText(user.optString("full_name"));
                defaultLocationEditText.setText(user.optString("default_location", sessionManager.getDefaultLocation()));
                emailTextView.setText("Email: " + user.optString("email"));
                userTypeTextView.setText("Account type: " + user.optString("user_type"));
            } catch (JSONException e) {
                Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAccount() {
        String userId = sessionManager.getUserId();
        String fullName = fullNameEditText.getText().toString().trim();
        String defaultLocation = defaultLocationEditText.getText().toString().trim();
        boolean boldTextEnabled = boldTextCheckBox.isChecked();

        if (TextUtils.isEmpty(fullName)) {
            fullNameEditText.setError("Enter your full name");
            return;
        }

        saveButton.setEnabled(false);
        FormBody.Builder formBuilder = new FormBody.Builder()
                .add("user_id", userId)
                .add("full_name", fullName)
                .add("default_location", defaultLocation);

        sessionManager.saveBoldTextEnabled(boldTextEnabled);
        applyBoldTextPreference();
        api.post("update_account.php", formBuilder, this::showSaveResult);
    }

    private void showSaveResult(boolean networkSuccess, String responseText) {
        runOnUiThread(() -> {
            saveButton.setEnabled(true);
            try {
                JSONObject jsonObject = new JSONObject(responseText);
                boolean success = jsonObject.optBoolean("success");
                String message = jsonObject.optString("message", "Could not save account");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (success) {
                    sessionManager.saveFullName(fullNameEditText.getText().toString().trim());
                    sessionManager.saveDefaultLocation(defaultLocationEditText.getText().toString().trim());
                }
            } catch (JSONException e) {
                Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDeleteAccount() {
        new AlertDialog.Builder(this)
                .setTitle("Delete account?")
                .setMessage("This will permanently delete your account and related records.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteAccount();
                    }
                })
                .show();
    }

    private void deleteAccount() {
        String userId = sessionManager.getUserId();
        deleteButton.setEnabled(false);
        api.post("delete_account.php", new FormBody.Builder().add("user_id", userId), this::showDeleteResult);
    }

    private void showDeleteResult(boolean networkSuccess, String responseText) {
        runOnUiThread(() -> {
            deleteButton.setEnabled(true);
            try {
                JSONObject jsonObject = new JSONObject(responseText);
                boolean success = jsonObject.optBoolean("success");
                String message = jsonObject.optString("message", "Could not delete account");
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                if (success) {
                    sessionManager.clear();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            } catch (JSONException e) {
                Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyBoldTextPreference() {
        if (sessionManager.isBoldTextEnabled()) {
            fullNameEditText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            defaultLocationEditText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            emailTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            userTypeTextView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            boldTextCheckBox.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        } else {
            fullNameEditText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            defaultLocationEditText.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            emailTextView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            userTypeTextView.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            boldTextCheckBox.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        }
    }
}
