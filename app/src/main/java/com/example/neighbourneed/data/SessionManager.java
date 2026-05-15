package com.example.neighbourneed.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "neighbourneed_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_DEFAULT_LOCATION = "default_location";
    private static final String KEY_BOLD_TEXT = "bold_text";

    private final SharedPreferences preferences;

    public SessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public String getUserId() {
        return preferences.getString(KEY_USER_ID, "");
    }

    public String getFullName() {
        return preferences.getString(KEY_FULL_NAME, "");
    }

    public void saveFullName(String fullName) {
        preferences.edit().putString(KEY_FULL_NAME, fullName).apply();
    }

    public String getDefaultLocation() {
        return preferences.getString(KEY_DEFAULT_LOCATION, "");
    }

    public void saveDefaultLocation(String defaultLocation) {
        preferences.edit().putString(KEY_DEFAULT_LOCATION, defaultLocation).apply();
    }

    public boolean isBoldTextEnabled() {
        return preferences.getBoolean(KEY_BOLD_TEXT, false);
    }

    public void saveBoldTextEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_BOLD_TEXT, enabled).apply();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
