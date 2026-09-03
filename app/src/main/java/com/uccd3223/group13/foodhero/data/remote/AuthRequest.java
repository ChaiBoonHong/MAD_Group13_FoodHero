package com.uccd3223.group13.foodhero.data.remote;

import com.google.gson.annotations.SerializedName;

public class AuthRequest {
    @SerializedName("email")
    private String email;

    @SerializedName("password")
    private String password;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("data")
    private java.util.Map<String, Object> data;

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public AuthRequest(String email, String password, java.util.Map<String, Object> data) {
        this.email = email;
        this.password = password;
        this.data = data;
    }

    public static AuthRequest forRefreshToken(String refreshToken) {
        AuthRequest req = new AuthRequest(null, null);
        req.refreshToken = refreshToken;
        return req;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
