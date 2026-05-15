package com.example.neighbourneed.ui.login;

/**
 * Class exposing authenticated user details to the UI.
 */
class LoggedInUserView {
    private String userId;
    private String displayName;
    private String userType;
    //... other data fields that may be accessible to the UI

    LoggedInUserView(String userId, String displayName, String userType) {
        this.userId = userId;
        this.displayName = displayName;
        this.userType = userType;
    }

    String getUserId() {
        return userId;
    }

    String getDisplayName() {
        return displayName;
    }

    String getUserType() {
        return userType;
    }
}
