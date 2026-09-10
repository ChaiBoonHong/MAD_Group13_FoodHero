package com.uccd3223.group13.foodhero.data.remote;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    @SerializedName("expires_in")
    private long expiresIn;

    @SerializedName("user")
    private SupabaseUser user;

    public static class SupabaseUser {
        @SerializedName("id")
        private String id;

        @SerializedName("email")
        private String email;

        @SerializedName("email_confirmed_at")
        private String emailConfirmedAt;

        public String getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public boolean isEmailConfirmed() {
            return emailConfirmedAt != null && !emailConfirmedAt.trim().isEmpty();
        }
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public SupabaseUser getUser() {
        return user;
    }
}
