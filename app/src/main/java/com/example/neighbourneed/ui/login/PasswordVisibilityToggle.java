package com.example.neighbourneed.ui.login;

import android.text.method.PasswordTransformationMethod;
import android.widget.EditText;
import android.widget.ImageButton;

import com.example.neighbourneed.R;

class PasswordVisibilityToggle {

    private PasswordVisibilityToggle() {
    }

    static void attach(EditText passwordEditText, ImageButton toggleButton) {
        final boolean[] visible = {false};
        toggleButton.setImageResource(R.drawable.ic_visibility);
        toggleButton.setContentDescription("Show password");
        toggleButton.setOnClickListener(view -> {
            visible[0] = !visible[0];
            int cursorPosition = passwordEditText.getSelectionStart();
            passwordEditText.setTransformationMethod(visible[0] ? null : PasswordTransformationMethod.getInstance());
            toggleButton.setImageResource(visible[0] ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
            toggleButton.setContentDescription(visible[0] ? "Hide password" : "Show password");
            passwordEditText.setSelection(Math.max(0, Math.min(cursorPosition, passwordEditText.length())));
        });
    }
}
