package com.uccd3223.group13.foodhero.data.session;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import com.google.gson.Gson;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.UserRole;

public class SessionManager {
    private static final String PREF_NAME = "foodhero_secure_session";
    private static final String KEY_ACCESS_TOKEN = "key_access_token";
    private static final String KEY_REFRESH_TOKEN = "key_refresh_token";
    private static final String KEY_USER_ID = "key_user_id";
    private static final String KEY_EMAIL = "key_email";
    private static final String KEY_USER_ROLE = "key_user_role";
    private static final String KEY_PROFILE_JSON = "key_profile_json";
    private static final String KEY_IS_LOGGED_IN = "key_is_logged_in";

    private static volatile SessionManager INSTANCE;
    private final SharedPreferences prefs;
    private final Gson gson;

    private SessionManager(Context context) {
        this.gson = new Gson();
        SharedPreferences securePrefs;
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            securePrefs = EncryptedSharedPreferences.create(
                PREF_NAME,
                masterKeyAlias,
                context.getApplicationContext(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to standard private preferences if keystore is unavailable on test runner
            securePrefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
        this.prefs = securePrefs;
    }

    public static SessionManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SessionManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SessionManager(context);
                }
            }
        }
        return INSTANCE;
    }

    public void saveSession(String accessToken, String refreshToken, Profile profile) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_ACCESS_TOKEN, accessToken);
        editor.putString(KEY_REFRESH_TOKEN, refreshToken);
        if (profile != null) {
            editor.putString(KEY_USER_ID, profile.getId());
            editor.putString(KEY_EMAIL, profile.getEmail());
            editor.putString(KEY_USER_ROLE, profile.getRole().getValue());
            editor.putString(KEY_PROFILE_JSON, gson.toJson(profile));
        }
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public void updateProfile(Profile profile) {
        if (profile != null) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_USER_ID, profile.getId());
            editor.putString(KEY_EMAIL, profile.getEmail());
            editor.putString(KEY_USER_ROLE, profile.getRole().getValue());
            editor.putString(KEY_PROFILE_JSON, gson.toJson(profile));
            editor.apply();
        }
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null;
    }

    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public UserRole getUserRole() {
        String roleStr = prefs.getString(KEY_USER_ROLE, "student");
        return UserRole.fromString(roleStr);
    }

    public Profile getProfile() {
        String profileJson = prefs.getString(KEY_PROFILE_JSON, null);
        if (profileJson != null) {
            try {
                return gson.fromJson(profileJson, Profile.class);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public String getFullName() {
        Profile p = getProfile();
        return p != null ? p.getFullName() : null;
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
