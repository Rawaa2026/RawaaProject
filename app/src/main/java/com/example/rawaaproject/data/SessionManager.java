package com.example.rawaaproject.data;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * وحدة مسؤولة عن حفظ واسترجاع حالة تسجيل الدخول محلياً.
 * تستخدم SharedPreferences ولا تتصل بالشبكة.
 */
public class SessionManager {

    private static final String PREFS_NAME = "rawaa_session";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ROLE = "user_role";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** حفظ بيانات الجلسة بعد تسجيل الدخول أو التسجيل الناجح */
    public void saveLogin(String userId, String email, String role) {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putString(KEY_USER_ROLE, role != null ? role : "")
                .apply();
    }

    /** مسح الجلسة (تسجيل الخروج) */
    public void clearSession() {
        prefs.edit()
                .putBoolean(KEY_LOGGED_IN, false)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_USER_ROLE)
                .apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    /** "teacher" أو "student" */
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "");
    }
}
