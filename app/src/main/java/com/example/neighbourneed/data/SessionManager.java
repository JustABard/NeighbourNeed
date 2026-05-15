package com.example.neighbourneed.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "neighbourneed_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";
    private static final String KEY_USER_TYPE = "user_type";
    private static final String KEY_DEFAULT_LOCATION = "default_location";
    private static final String KEY_DEFAULT_LATITUDE = "default_latitude";
    private static final String KEY_DEFAULT_LONGITUDE = "default_longitude";
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

    public String getUserType() {
        return preferences.getString(KEY_USER_TYPE, "");
    }

    public void saveLogin(String userId, String fullName, String userType) {
        preferences.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_FULL_NAME, fullName)
                .putString(KEY_USER_TYPE, userType)
                .apply();
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

    public String getDefaultLatitude() {
        return preferences.getString(KEY_DEFAULT_LATITUDE, "");
    }

    public String getDefaultLongitude() {
        return preferences.getString(KEY_DEFAULT_LONGITUDE, "");
    }

    public void saveDefaultCoordinates(String latitude, String longitude) {
        preferences.edit()
                .putString(KEY_DEFAULT_LATITUDE, latitude)
                .putString(KEY_DEFAULT_LONGITUDE, longitude)
                .apply();
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
