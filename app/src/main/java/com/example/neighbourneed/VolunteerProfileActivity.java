package com.example.neighbourneed;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.FormBody;

public class VolunteerProfileActivity extends AppCompatActivity {

    private static final int PICK_PROFILE_IMAGE = 77;

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private TextView profileHeader;
    private ImageView profileImageView;
    private LinearLayout thanksList;
    private EditText thanksMessage;
    private EditText ratingInput;
    private Button postThanksButton;
    private Button uploadPhotoButton;
    private String shopperUserId;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volunteer_profile);

        sessionManager = new SessionManager(this);
        shopperUserId = getIntent().getStringExtra("shopper_user_id");
        if ((shopperUserId == null || shopperUserId.isEmpty()) && "shopper".equals(sessionManager.getUserType())) {
            shopperUserId = sessionManager.getUserId();
        }
        orderId = getIntent().getStringExtra("order_id");

        profileHeader = findViewById(R.id.volunteer_profile_header);
        profileImageView = findViewById(R.id.profile_image);
        thanksList = findViewById(R.id.thanks_list);
        thanksMessage = findViewById(R.id.thanks_message);
        ratingInput = findViewById(R.id.rating_input);
        postThanksButton = findViewById(R.id.post_thanks);
        uploadPhotoButton = findViewById(R.id.upload_profile_photo);

        postThanksButton.setVisibility("customer".equals(sessionManager.getUserType()) ? View.VISIBLE : View.GONE);
        thanksMessage.setVisibility("customer".equals(sessionManager.getUserType()) ? View.VISIBLE : View.GONE);
        ratingInput.setVisibility("customer".equals(sessionManager.getUserType()) ? View.VISIBLE : View.GONE);
        uploadPhotoButton.setVisibility(sessionManager.getUserId().equals(shopperUserId) ? View.VISIBLE : View.GONE);
        postThanksButton.setOnClickListener(view -> postThanks());
        uploadPhotoButton.setOnClickListener(view -> pickProfileImage());

        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadProfile() {
        if (shopperUserId == null || shopperUserId.isEmpty()) {
            profileHeader.setText("Volunteer not found");
            return;
        }

        api.post("volunteer_profile.php",
                new FormBody.Builder().add("shopper_user_id", shopperUserId),
                (networkSuccess, responseText) -> runOnUiThread(() -> showProfile(responseText)));
    }

    private void showProfile(String responseText) {
        thanksList.removeAllViews();
        try {
            JSONObject response = new JSONObject(responseText);
            if (!response.optBoolean("success")) {
                profileHeader.setText(response.optString("message", "Could not load profile"));
                return;
            }

            JSONObject profile = response.getJSONObject("profile");
            profileHeader.setText(profile.optString("full_name") +
                    "\nVehicle: " + profile.optString("vehicle_type") +
                    "\nRating: " + profile.optString("average_rating", "0") +
                    " (" + profile.optString("rating_count", "0") + ")");

            showProfileImage(profile.optString("profile_image_base64"));

            JSONArray thanks = response.getJSONArray("thanks");
            if (thanks.length() == 0) {
                addThanksText("No thank-you messages yet.");
                return;
            }

            for (int i = 0; i < thanks.length(); i++) {
                JSONObject item = thanks.getJSONObject(i);
                addThanksText(item.optString("message") +
                        "\n- " + item.optString("customer_name") +
                        "\n" + item.optString("created_at"));
            }
        } catch (JSONException e) {
            profileHeader.setText(responseText);
        }
    }

    private void postThanks() {
        String message = thanksMessage.getText().toString().trim();
        String rating = ratingInput.getText().toString().trim();
        if (TextUtils.isEmpty(message) && TextUtils.isEmpty(rating)) {
            thanksMessage.setError("Enter a message or rating");
            return;
        }

        if (!TextUtils.isEmpty(rating)) {
            try {
                int ratingValue = Integer.parseInt(rating);
                if (ratingValue < 1 || ratingValue > 5) {
                    ratingInput.setError("Rating must be 1-5");
                    return;
                }
            } catch (NumberFormatException e) {
                ratingInput.setError("Rating must be 1-5");
                return;
            }
        }

        if (TextUtils.isEmpty(orderId)) {
            Toast.makeText(this, "Open this from a completed order to rate", Toast.LENGTH_SHORT).show();
            return;
        }

        postThanksButton.setEnabled(false);
        api.post("post_thanks.php",
                new FormBody.Builder()
                        .add("order_id", orderId == null ? "" : orderId)
                        .add("customer_user_id", sessionManager.getUserId())
                        .add("shopper_user_id", shopperUserId == null ? "" : shopperUserId)
                        .add("message", message)
                        .add("rating", rating),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    postThanksButton.setEnabled(true);
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            thanksMessage.setText("");
                            ratingInput.setText("");
                            loadProfile();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void addThanksText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(14, 14, 14, 14);
        thanksList.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }

    private void pickProfileImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select profile photo"), PICK_PROFILE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PROFILE_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            uploadProfileImage(data.getData());
        }
    }

    private void uploadProfileImage(Uri uri) {
        try {
            byte[] bytes = readBytes(uri);
            String encoded = Base64.encodeToString(bytes, Base64.NO_WRAP);
            api.post("update_shopper_profile.php",
                    new FormBody.Builder()
                            .add("user_id", sessionManager.getUserId())
                            .add("profile_image_base64", encoded),
                    (networkSuccess, responseText) -> runOnUiThread(() -> {
                        try {
                            JSONObject response = new JSONObject(responseText);
                            Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                            if (response.optBoolean("success")) {
                                loadProfile();
                            }
                        } catch (JSONException e) {
                            Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                        }
                    }));
        } catch (IOException e) {
            Toast.makeText(this, "Could not read selected image", Toast.LENGTH_SHORT).show();
        }
    }

    private byte[] readBytes(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        inputStream.close();
        return outputStream.toByteArray();
    }

    private void showProfileImage(String encoded) {
        if (encoded == null || encoded.isEmpty() || "null".equals(encoded)) {
            profileImageView.setImageResource(R.drawable.logo);
            return;
        }

        byte[] bytes = Base64.decode(encoded, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap != null) {
            profileImageView.setImageBitmap(bitmap);
        }
    }
}
