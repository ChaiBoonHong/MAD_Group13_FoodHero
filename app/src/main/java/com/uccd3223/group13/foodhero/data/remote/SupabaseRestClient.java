package com.uccd3223.group13.foodhero.data.remote;

import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.Review;
import com.uccd3223.group13.foodhero.data.model.ServiceArea;
import java.util.List;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseRestClient {
    // --- PROFILES ---
    @GET("/rest/v1/profiles")
    Call<List<Profile>> getProfile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("/rest/v1/profiles")
    Call<List<Profile>> createProfile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Body Profile profile
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @PATCH("/rest/v1/profiles")
    Call<List<Profile>> updateProfile(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery,
        @Body Profile profile
    );

    // --- MERCHANTS ---
    @GET("/rest/v1/merchants")
    Call<List<Merchant>> getMerchants(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer
    );

    @GET("/rest/v1/merchants")
    Call<List<Merchant>> getMerchantByOwner(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("owner_id") String ownerQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("/rest/v1/merchants")
    Call<List<Merchant>> createMerchant(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Body Merchant merchant
    );

    // --- LISTINGS ---
    @GET("/rest/v1/listings?select=*,merchants(*)")
    Call<List<Listing>> getActiveListings(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("status") String statusQuery,
        @Query("order") String orderQuery
    );

    @GET("/rest/v1/listings")
    Call<List<Listing>> getMerchantListings(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("merchant_id") String merchantQuery,
        @Query("order") String orderQuery
    );

    @GET("/rest/v1/listings?select=*,merchants(*)")
    Call<List<Listing>> getListingById(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("/rest/v1/listings")
    Call<List<Listing>> createListing(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Body Listing listing
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @PATCH("/rest/v1/listings")
    Call<List<Listing>> updateListing(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery,
        @Body Listing listing
    );

    // --- ORDERS ---
    @GET("/rest/v1/orders?select=*,listings(*),merchants(*)")
    Call<List<Order>> getStudentOrders(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("student_id") String studentQuery,
        @Query("order") String orderQuery
    );

    @GET("/rest/v1/orders?select=*,listings(*),profiles(*)")
    Call<List<Order>> getMerchantOrders(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("merchant_id") String merchantQuery,
        @Query("order") String orderQuery
    );

    @GET("/rest/v1/orders?select=*,listings(*),profiles(*)")
    Call<List<Order>> getOrderByCode(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("order_code") String codeQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("/rest/v1/orders")
    Call<List<Order>> createOrder(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Body Order order
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @PATCH("/rest/v1/orders")
    Call<List<Order>> updateOrderStatus(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery,
        @Body RequestBody body
    );

    // --- REVIEWS ---
    @GET("/rest/v1/reviews?select=*,profiles(*),listings(*)")
    Call<List<Review>> getMerchantReviews(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("merchant_id") String merchantQuery,
        @Query("order") String orderQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("/rest/v1/reviews")
    Call<List<Review>> submitReview(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Body Review review
    );

    // --- NOTIFICATIONS ---
    @GET("/rest/v1/notifications")
    Call<List<FoodHeroNotification>> getNotifications(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("recipient_id") String recipientQuery,
        @Query("order") String orderQuery
    );

    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @PATCH("/rest/v1/notifications")
    Call<List<FoodHeroNotification>> markNotificationRead(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer,
        @Query("id") String idQuery,
        @Body RequestBody body
    );

    // --- SERVICE AREAS & CAMPUS LANDMARKS ---
    @GET("/rest/v1/service_areas?is_active=eq.true")
    Call<List<ServiceArea>> getServiceAreas(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer
    );

    @GET("/rest/v1/campus_landmarks?is_active=eq.true")
    Call<List<CampusLandmark>> getCampusLandmarks(
        @Header("apikey") String apiKey,
        @Header("Authorization") String bearer
    );
}
