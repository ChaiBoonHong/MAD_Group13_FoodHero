package com.uccd3223.group13.foodhero.data.remote;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseAuthService {
    @Headers({"Content-Type: application/json"})
    @POST("/auth/v1/signup")
    Call<AuthResponse> signUp(
        @Header("apikey") String apiKey,
        @Body AuthRequest request
    );

    @Headers({"Content-Type: application/json"})
    @POST("/auth/v1/token?grant_type=password")
    Call<AuthResponse> signInWithPassword(
        @Header("apikey") String apiKey,
        @Body AuthRequest request
    );

    @Headers({"Content-Type: application/json"})
    @POST("/auth/v1/token?grant_type=id_token")
    Call<AuthResponse> signInWithIdToken(
        @Header("apikey") String apiKey,
        @Body AuthIdTokenRequest request
    );

    @Headers({"Content-Type: application/json"})
    @POST("/auth/v1/token?grant_type=refresh_token")
    Call<AuthResponse> refreshToken(
        @Header("apikey") String apiKey,
        @Body AuthRequest request
    );

    @Headers({"Content-Type: application/json"})
    @POST("/auth/v1/recover")
    Call<ResponseBody> resetPasswordForEmail(
        @Header("apikey") String apiKey,
        @Body AuthRequest request
    );

    @POST("/auth/v1/logout")
    Call<ResponseBody> logout(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken
    );

    @retrofit2.http.GET("/auth/v1/user")
    Call<AuthResponse.SupabaseUser> getUser(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearerToken
    );
}
