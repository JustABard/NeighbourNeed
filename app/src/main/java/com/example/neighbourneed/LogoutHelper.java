package com.example.neighbourneed;

import android.content.Context;
import android.content.Intent;

import com.example.neighbourneed.data.SessionManager;
import com.example.neighbourneed.ui.login.LoginActivity;

class LogoutHelper {

    static void logout(Context context, SessionManager sessionManager) {
        sessionManager.clear();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }
}
