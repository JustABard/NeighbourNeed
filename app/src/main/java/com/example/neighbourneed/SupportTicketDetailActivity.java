package com.example.neighbourneed;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class SupportTicketDetailActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private SessionManager sessionManager;
    private String ticketId;
    private TextView titleTextView;
    private TextView statusTextView;
    private TextView messagesTextView;
    private EditText replyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_ticket_detail);

        sessionManager = new SessionManager(this);
        ticketId = getIntent().getStringExtra("ticket_id");
        titleTextView = findViewById(R.id.support_ticket_title);
        statusTextView = findViewById(R.id.support_ticket_status);
        messagesTextView = findViewById(R.id.support_ticket_messages);
        replyInput = findViewById(R.id.support_reply_input);
        Button sendButton = findViewById(R.id.send_support_reply);
        Button refreshButton = findViewById(R.id.refresh_support_ticket);
        Button closeButton = findViewById(R.id.close_support_ticket);

        sendButton.setOnClickListener(view -> sendReply());
        refreshButton.setOnClickListener(view -> loadTicket());
        closeButton.setOnClickListener(view -> closeTicket());

        loadTicket();
    }

    @Override
    protected void onResume() {
        super.onResume();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadTicket() {
        if (TextUtils.isEmpty(ticketId)) {
            messagesTextView.setText("Missing ticket");
            return;
        }

        messagesTextView.setText("Loading...");
        api.post("support_ticket_details.php",
                new FormBody.Builder()
                        .add("ticket_id", ticketId)
                        .add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        if (!response.optBoolean("success")) {
                            messagesTextView.setText(response.optString("message", "Could not load ticket"));
                            return;
                        }

                        JSONObject ticket = response.getJSONObject("ticket");
                        titleTextView.setText("Ticket #" + ticket.optString("ticket_id"));
                        statusTextView.setText(ticket.optString("subject") +
                                "\nStatus: " + ticket.optString("status") +
                                "\nFrom: " + ticket.optString("full_name"));
                        renderMessages(response.getJSONArray("messages"));
                        UiPreferences.apply(findViewById(R.id.root), sessionManager);
                    } catch (JSONException e) {
                        messagesTextView.setText(responseText);
                    }
                }));
    }

    private void renderMessages(JSONArray messages) throws JSONException {
        if (messages.length() == 0) {
            messagesTextView.setText("No messages yet.");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.getJSONObject(i);
            builder.append(message.optString("sender_name"))
                    .append(" (")
                    .append(message.optString("sender_type"))
                    .append("):\n")
                    .append(message.optString("message"))
                    .append("\n\n");
        }
        messagesTextView.setText(builder.toString().trim());
    }

    private void sendReply() {
        String message = replyInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            replyInput.setError("Enter a reply");
            return;
        }

        api.post("post_support_message.php",
                new FormBody.Builder()
                        .add("ticket_id", ticketId)
                        .add("user_id", sessionManager.getUserId())
                        .add("message", message),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            replyInput.setText("");
                            loadTicket();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void closeTicket() {
        if (TextUtils.isEmpty(ticketId)) {
            return;
        }

        api.post("close_support_ticket.php",
                new FormBody.Builder()
                        .add("ticket_id", ticketId)
                        .add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            finish();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }
}
