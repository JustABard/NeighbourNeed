package com.example.neighbourneed;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.neighbourneed.data.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.FormBody;

public class SupportTicketsActivity extends AppCompatActivity {

    private final CustomerApi api = new CustomerApi();
    private LinearLayout listLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_card);

        sessionManager = new SessionManager(this);
        ((TextView) findViewById(R.id.list_title)).setText("Support Tickets");
        listLayout = findViewById(R.id.list_container);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTickets();
        UiPreferences.apply(findViewById(R.id.root), sessionManager);
    }

    private void loadTickets() {
        listLayout.removeAllViews();
        if (!"admin".equals(sessionManager.getUserType())) {
            addCreateButton();
        }
        addText("Loading...");

        api.post("support_tickets.php",
                new FormBody.Builder().add("user_id", sessionManager.getUserId()),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    listLayout.removeAllViews();
                    if (!"admin".equals(sessionManager.getUserType())) {
                        addCreateButton();
                    }

                    try {
                        JSONObject response = new JSONObject(responseText);
                        if (!response.optBoolean("success")) {
                            addText(response.optString("message", "Could not load support tickets"));
                            return;
                        }

                        JSONArray tickets = response.getJSONArray("tickets");
                        if (tickets.length() == 0) {
                            addText("No support tickets yet.");
                            return;
                        }

                        for (int i = 0; i < tickets.length(); i++) {
                            addTicket(tickets.getJSONObject(i));
                        }
                        UiPreferences.apply(findViewById(R.id.root), sessionManager);
                    } catch (JSONException e) {
                        addText(responseText);
                    }
                }));
    }

    private void addCreateButton() {
        Button button = new Button(this);
        button.setText("New Support Ticket");
        button.setTextColor(0xFF071B25);
        button.setTextSize(16);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF84C8FF));
        button.setOnClickListener(view -> showCreateTicketDialog());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(58)
        );
        params.setMargins(0, 0, 0, dp(14));
        listLayout.addView(button, params);
        UiPreferences.apply(button, sessionManager);
    }

    private void addTicket(JSONObject ticket) {
        CardView cardView = new CardView(this);
        cardView.setCardBackgroundColor(Color.WHITE);
        cardView.setRadius(dp(14));
        cardView.setCardElevation(dp(4));
        cardView.setUseCompatPadding(true);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));

        TextView textView = new TextView(this);
        String ownerLine = "admin".equals(sessionManager.getUserType())
                ? "\nFrom: " + ticket.optString("full_name") + " (" + ticket.optString("user_type") + ")"
                : "";
        String lastMessage = ticket.optString("last_message");
        textView.setText("Ticket #" + ticket.optString("ticket_id") +
                "\n" + ticket.optString("subject") +
                ownerLine +
                "\nStatus: " + ticket.optString("status") +
                "\nUpdated: " + ticket.optString("updated_at") +
                (TextUtils.isEmpty(lastMessage) || "null".equals(lastMessage) ? "" : "\n\nLast: " + lastMessage));
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(dp(18), dp(16), dp(18), dp(16));

        cardView.setOnClickListener(view -> {
            Intent intent = new Intent(this, SupportTicketDetailActivity.class);
            intent.putExtra("ticket_id", ticket.optString("ticket_id"));
            startActivity(intent);
        });
        cardView.addView(textView);
        listLayout.addView(cardView, params);
        UiPreferences.apply(textView, sessionManager);
    }

    private void showCreateTicketDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(18), dp(8), dp(18), 0);

        EditText subjectInput = new EditText(this);
        subjectInput.setHint("Subject");
        subjectInput.setSingleLine(true);
        layout.addView(subjectInput);

        EditText messageInput = new EditText(this);
        messageInput.setHint("Describe the issue");
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        messageInput.setMinLines(3);
        layout.addView(messageInput);

        new AlertDialog.Builder(this)
                .setTitle("New Support Ticket")
                .setView(layout)
                .setPositiveButton("Submit", (dialog, which) -> createTicket(
                        subjectInput.getText().toString().trim(),
                        messageInput.getText().toString().trim()
                ))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTicket(String subject, String message) {
        if (TextUtils.isEmpty(subject) || TextUtils.isEmpty(message)) {
            Toast.makeText(this, "Enter a subject and message", Toast.LENGTH_SHORT).show();
            return;
        }

        api.post("create_support_ticket.php",
                new FormBody.Builder()
                        .add("user_id", sessionManager.getUserId())
                        .add("subject", subject)
                        .add("message", message),
                (networkSuccess, responseText) -> runOnUiThread(() -> {
                    try {
                        JSONObject response = new JSONObject(responseText);
                        Toast.makeText(this, response.optString("message", "Done"), Toast.LENGTH_SHORT).show();
                        if (response.optBoolean("success")) {
                            loadTickets();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, responseText, Toast.LENGTH_SHORT).show();
                    }
                }));
    }

    private void addText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextColor(0xFF102A3A);
        textView.setTextSize(16);
        textView.setPadding(12, 12, 12, 12);
        listLayout.addView(textView);
        UiPreferences.apply(textView, sessionManager);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
