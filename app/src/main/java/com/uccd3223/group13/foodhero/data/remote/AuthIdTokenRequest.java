package com.uccd3223.group13.foodhero.data.remote;

import com.google.gson.annotations.SerializedName;
import java.util.Map;

public class AuthIdTokenRequest {
    @SerializedName("provider")
    private String provider;

    @SerializedName("id_token")
    private String idToken;

    @SerializedName("nonce")
    private String nonce;

    @SerializedName("data")
    private Map<String, Object> data;

    public AuthIdTokenRequest(String provider, String idToken) {
        this.provider = provider;
        this.idToken = idToken;
    }

    public AuthIdTokenRequest(String provider, String idToken, Map<String, Object> data) {
        this.provider = provider;
        this.idToken = idToken;
        this.data = data;
    }

    public String getProvider() {
        return provider;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
