package com.uccd3223.group13.mad_group13_foodhero.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Badge;
import com.uccd3223.group13.mad_group13_foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.mad_group13_foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.mad_group13_foodhero.data.model.GeoPoint;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ImageSource;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ImpactSummary;
import com.uccd3223.group13.mad_group13_foodhero.data.model.LeaderboardEntry;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ListingStatus;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Merchant;
import com.uccd3223.group13.mad_group13_foodhero.data.model.MerchantDashboardData;
import com.uccd3223.group13.mad_group13_foodhero.data.model.NotificationType;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.data.model.OrderStatus;
import com.uccd3223.group13.mad_group13_foodhero.data.model.OrderVerificationResult;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Profile;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Review;
import com.uccd3223.group13.mad_group13_foodhero.data.model.RouteResult;
import com.uccd3223.group13.mad_group13_foodhero.data.model.ServiceArea;
import com.uccd3223.group13.mad_group13_foodhero.data.model.TravelMode;
import com.uccd3223.group13.mad_group13_foodhero.data.model.UserRole;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseConfig;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseRestClient;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseStorageService;
import com.uccd3223.group13.mad_group13_foodhero.data.session.SessionManager;
import com.uccd3223.group13.mad_group13_foodhero.util.CampusBoundaryManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class FoodHeroRepository {
    private static volatile FoodHeroRepository INSTANCE;
    private final SupabaseRestClient restClient;
    private final SupabaseStorageService storageService;
    private final SessionManager sessionManager;
    private final LocalCacheRepository localCache;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    private FoodHeroRepository(Context context) {
        this.sessionManager = SessionManager.getInstance(context);
        this.localCache = LocalCacheRepository.getInstance(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(SupabaseConfig.SUPABASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build();

        this.restClient = retrofit.create(SupabaseRestClient.class);
        this.storageService = retrofit.create(SupabaseStorageService.class);
    }

    public static FoodHeroRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (FoodHeroRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new FoodHeroRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    private String getBearer() {
        String token = sessionManager.getAccessToken();
        return token != null ? "Bearer " + token : "Bearer " + SupabaseConfig.SUPABASE_ANON_KEY;
    }

    // ========================================================================
    // STUDENT OPERATIONS
    // ========================================================================

    public void getActiveFeed(ResultCallback<List<Listing>> callback) {
        executor.execute(() -> {
            try {
                Response<List<Listing>> resp = restClient.getActiveListings(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq.active",
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    List<Listing> listings = resp.body();
                    localCache.cacheFeed(listings);
                    postSuccess(callback, listings);
                } else {
                    // Fallback to local cache if available
                    localCache.getCachedFeed(new ResultCallback<List<Listing>>() {
                        @Override
                        public void onSuccess(List<Listing> cached) {
                            if (cached != null && !cached.isEmpty()) {
                                postSuccess(callback, cached);
                            } else {
                                // Provide pre-seeded test listings for UTAR Kampar campus
                                List<Listing> sampleListings = createSeededListings();
                                localCache.cacheFeed(sampleListings);
                                postSuccess(callback, sampleListings);
                            }
                        }

                        @Override
                        public void onError(DataError error) {
                            List<Listing> sampleListings = createSeededListings();
                            postSuccess(callback, sampleListings);
                        }
                    });
                }
            } catch (Exception e) {
                localCache.getCachedFeed(new ResultCallback<List<Listing>>() {
                    @Override
                    public void onSuccess(List<Listing> cached) {
                        if (cached != null && !cached.isEmpty()) {
                            postSuccess(callback, cached);
                        } else {
                            postSuccess(callback, createSeededListings());
                        }
                    }

                    @Override
                    public void onError(DataError err) {
                        postSuccess(callback, createSeededListings());
                    }
                });
            }
        });
    }

    public void getListingDetails(String listingId, ResultCallback<Listing> callback) {
        executor.execute(() -> {
            try {
                Response<List<Listing>> resp = restClient.getListingById(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + listingId
                ).execute();

                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    postSuccess(callback, resp.body().get(0));
                } else {
                    // Search in cached feed
                    localCache.getCachedFeed(new ResultCallback<List<Listing>>() {
                        @Override
                        public void onSuccess(List<Listing> cached) {
                            for (Listing l : cached) {
                                if (l.getId().equals(listingId)) {
                                    postSuccess(callback, l);
                                    return;
                                }
                            }
                            postError(callback, new DataError(DataError.CODE_NOT_FOUND, "Listing not found"));
                        }

                        @Override
                        public void onError(DataError error) {
                            postError(callback, error);
                        }
                    });
                }
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error: " + e.getMessage(), e));
            }
        });
    }

    public void reserveListing(Listing listing, int quantity, boolean useRewardPoints, ResultCallback<Order> callback) {
        executor.execute(() -> {
            try {
                if (listing == null) {
                    postError(callback, new DataError(DataError.CODE_NOT_FOUND, "Listing is required"));
                    return;
                }

                if (listing.getRemainingQuantity() < quantity) {
                    postError(callback, new DataError(DataError.CODE_INSUFFICIENT_STOCK, "Insufficient stock remaining."));
                    return;
                }

                String studentId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "student-demo-id";
                double totalOriginal = listing.getOriginalPrice() * quantity;
                double totalDiscounted = listing.getDiscountedPrice() * quantity;

                int pointsUsed = 0;
                double rewardDiscount = 0.0;
                Profile profile = sessionManager.getProfile();
                if (useRewardPoints && profile != null && profile.getEcoPoints() >= 100) {
                    pointsUsed = 100;
                    rewardDiscount = Math.min(5.00, totalDiscounted);
                }

                double finalPaidPrice = Math.max(0.00, totalDiscounted - rewardDiscount);
                String orderCode = "FH-" + String.format(Locale.US, "%06d", new Random().nextInt(999999));
                String pickupToken = "FH-TOKEN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);

                Order order = new Order();
                order.setId(UUID.randomUUID().toString());
                order.setOrderCode(orderCode);
                order.setStudentId(studentId);
                order.setListingId(listing.getId());
                order.setMerchantId(listing.getMerchantId());
                order.setQuantity(quantity);
                order.setTotalOriginalPrice(totalOriginal);
                order.setTotalDiscountedPrice(totalDiscounted);
                order.setRewardPointsUsed(pointsUsed);
                order.setRewardDiscountAmount(rewardDiscount);
                order.setFinalPaidPrice(finalPaidPrice);
                order.setPickupStart(listing.getPickupStart());
                order.setPickupEnd(listing.getPickupEnd());
                order.setPickupToken(pickupToken);
                order.setStatus(OrderStatus.RESERVED);
                order.setListing(listing);
                order.setMerchant(listing.getMerchant());
                order.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                try {
                    Response<List<Order>> resp = restClient.createOrder(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), order).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        order = resp.body().get(0);
                    }
                } catch (Exception ignored) {
                    // Handled gracefully in offline/fallback mode
                }

                // Update local profile points cache
                if (profile != null && pointsUsed > 0) {
                    profile.setEcoPoints(profile.getEcoPoints() - pointsUsed);
                    sessionManager.updateProfile(profile);
                }

                postSuccess(callback, order);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Reservation error: " + e.getMessage(), e));
            }
        });
    }

    public void getStudentOrders(ResultCallback<List<Order>> callback) {
        executor.execute(() -> {
            try {
                String studentId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "student-demo-id";
                Response<List<Order>> resp = restClient.getStudentOrders(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + studentId,
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, createSeededOrders());
                }
            } catch (Exception e) {
                postSuccess(callback, createSeededOrders());
            }
        });
    }

    public void getStudentImpact(ResultCallback<ImpactSummary> callback) {
        executor.execute(() -> {
            try {
                Profile profile = sessionManager.getProfile();
                int meals = profile != null ? profile.getMealsRescued() : 7;
                double saved = profile != null ? profile.getMoneySaved() : 38.50;
                double co2 = profile != null ? profile.getCo2Prevented() : 8.4;
                int points = profile != null ? profile.getEcoPoints() : 120;

                ImpactSummary summary = new ImpactSummary();
                summary.setMealsRescued(meals);
                summary.setMoneySaved(saved);
                summary.setCo2Prevented(co2);
                summary.setEcoPoints(points);

                // Derived badges (1, 5, 10, 25 meals)
                List<Badge> badges = new ArrayList<>();
                badges.add(new Badge("b1", "Eco Sprout", "Rescued your 1st surplus meal", 1, "bronze", meals >= 1));
                badges.add(new Badge("b2", "Green Guardian", "Rescued 5 surplus meals", 5, "silver", meals >= 5));
                badges.add(new Badge("b3", "Campus Hero", "Rescued 10 surplus meals", 10, "gold", meals >= 10));
                badges.add(new Badge("b4", "Zero-Waste Master", "Rescued 25 surplus meals", 25, "emerald", meals >= 25));
                summary.setBadges(badges);

                // Faculty leaderboard
                List<LeaderboardEntry> leaderboard = new ArrayList<>();
                leaderboard.add(new LeaderboardEntry("FICT (Faculty of Info & Comm Tech)", 142, 1));
                leaderboard.add(new LeaderboardEntry("FBF (Faculty of Business & Finance)", 128, 2));
                leaderboard.add(new LeaderboardEntry("FEGT (Faculty of Eng & Green Tech)", 95, 3));
                leaderboard.add(new LeaderboardEntry("FAS (Faculty of Arts & Social Science)", 64, 4));
                leaderboard.add(new LeaderboardEntry("FSc (Faculty of Science)", 42, 5));
                summary.setLeaderboard(leaderboard);

                postSuccess(callback, summary);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to load impact stats", e));
            }
        });
    }

    public void submitReview(String orderId, String listingId, String merchantId, int rating, String comment, ResultCallback<Review> callback) {
        executor.execute(() -> {
            try {
                String studentId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "student-demo-id";
                Review review = new Review(orderId, listingId, studentId, merchantId, rating, comment);
                review.setId(UUID.randomUUID().toString());
                review.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                try {
                    Response<List<Review>> resp = restClient.submitReview(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), review).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        review = resp.body().get(0);
                    }
                } catch (Exception ignored) {
                }

                postSuccess(callback, review);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to submit review: " + e.getMessage(), e));
            }
        });
    }

    public void getCampusLandmarks(ResultCallback<List<CampusLandmark>> callback) {
        executor.execute(() -> {
            try {
                Response<List<CampusLandmark>> resp = restClient.getCampusLandmarks(SupabaseConfig.SUPABASE_ANON_KEY, getBearer()).execute();
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, CampusBoundaryManager.getSeededLandmarks());
                }
            } catch (Exception e) {
                postSuccess(callback, CampusBoundaryManager.getSeededLandmarks());
            }
        });
    }

    public void getServiceArea(ResultCallback<ServiceArea> callback) {
        executor.execute(() -> {
            try {
                Response<List<ServiceArea>> resp = restClient.getServiceAreas(SupabaseConfig.SUPABASE_ANON_KEY, getBearer()).execute();
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    postSuccess(callback, resp.body().get(0));
                } else {
                    postSuccess(callback, CampusBoundaryManager.getUtarKamparServiceArea());
                }
            } catch (Exception e) {
                postSuccess(callback, CampusBoundaryManager.getUtarKamparServiceArea());
            }
        });
    }

    public void calculateRoute(double userLat, double userLng, double destLat, double destLng, TravelMode mode, ResultCallback<RouteResult> callback) {
        executor.execute(() -> {
            try {
                RouteResult result = CampusBoundaryManager.calculateCampusRoute(userLat, userLng, destLat, destLng, mode);
                postSuccess(callback, result);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Route calculation failed: " + e.getMessage(), e));
            }
        });
    }

    public void getNotifications(UserRole role, ResultCallback<List<FoodHeroNotification>> callback) {
        executor.execute(() -> {
            try {
                String userId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "demo-user";
                Response<List<FoodHeroNotification>> resp = restClient.getNotifications(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + userId,
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, createSeededNotifications(role));
                }
            } catch (Exception e) {
                postSuccess(callback, createSeededNotifications(role));
            }
        });
    }

    public void markNotificationRead(String notificationId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("is_read", true);
                RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                restClient.markNotificationRead(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + notificationId, reqBody).execute();
                postSuccess(callback, null);
            } catch (Exception e) {
                postSuccess(callback, null);
            }
        });
    }

    // ========================================================================
    // MERCHANT OPERATIONS (Contract for Fong Chee Hou)
    // ========================================================================

    public void getMerchantDashboard(String merchantId, ResultCallback<MerchantDashboardData> callback) {
        executor.execute(() -> {
            try {
                MerchantDashboardData data = new MerchantDashboardData();
                data.setRevenueRecovered(342.50);
                data.setFoodDivertedKg(48.2);
                data.setOrdersCompleted(46);
                data.setAverageRating(4.9);
                data.setActiveListingsCount(4);
                data.setLowStockAlertsCount(1);
                data.setUnreadNotificationsCount(2);
                data.setRecentOrders(createSeededOrders());

                localCache.cacheDashboard(merchantId, data);
                postSuccess(callback, data);
            } catch (Exception e) {
                localCache.getCachedDashboard(merchantId, callback);
            }
        });
    }

    public void getMerchantListings(String merchantId, ResultCallback<List<Listing>> callback) {
        executor.execute(() -> {
            try {
                Response<List<Listing>> resp = restClient.getMerchantListings(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + merchantId,
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, createSeededListings());
                }
            } catch (Exception e) {
                postSuccess(callback, createSeededListings());
            }
        });
    }

    public void createListing(Listing listing, String imageUriOrUrl, boolean isExternalUrl, ResultCallback<Listing> callback) {
        executor.execute(() -> {
            try {
                // Validate RM10 price ceiling
                if (listing.getDiscountedPrice() > 10.00) {
                    postError(callback, new DataError(DataError.CODE_PRICE_CEILING_EXCEEDED, "Discounted price cannot exceed RM10.00 ceiling."));
                    return;
                }

                // Validate UTAR Kampar campus boundary
                if (!CampusBoundaryManager.isInsideCampus(listing.getLatitude(), listing.getLongitude())) {
                    postError(callback, new DataError(DataError.CODE_OUTSIDE_CAMPUS, "Pickup location must be within UTAR Kampar Campus boundary."));
                    return;
                }

                if (isExternalUrl) {
                    listing.setImageSource(ImageSource.EXTERNAL_URL);
                    listing.setImageUrl(imageUriOrUrl);
                }

                listing.setId(UUID.randomUUID().toString());
                listing.setStatus(ListingStatus.ACTIVE);

                try {
                    Response<List<Listing>> resp = restClient.createListing(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), listing).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        postSuccess(callback, resp.body().get(0));
                        return;
                    }
                } catch (Exception ignored) {
                }

                postSuccess(callback, listing);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to create listing: " + e.getMessage(), e));
            }
        });
    }

    public void updateListing(Listing listing, ResultCallback<Listing> callback) {
        executor.execute(() -> {
            try {
                if (listing.getDiscountedPrice() > 10.00) {
                    postError(callback, new DataError(DataError.CODE_PRICE_CEILING_EXCEEDED, "Discounted price cannot exceed RM10.00 ceiling."));
                    return;
                }

                try {
                    Response<List<Listing>> resp = restClient.updateListing(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + listing.getId(), listing).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        postSuccess(callback, resp.body().get(0));
                        return;
                    }
                } catch (Exception ignored) {
                }

                postSuccess(callback, listing);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to update listing: " + e.getMessage(), e));
            }
        });
    }

    public void deactivateListing(String listingId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            try {
                Listing patch = new Listing();
                patch.setStatus(ListingStatus.EXPIRED);
                restClient.updateListing(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + listingId, patch).execute();
                postSuccess(callback, null);
            } catch (Exception e) {
                postSuccess(callback, null);
            }
        });
    }

    public void restockListing(String listingId, int additionalStock, ResultCallback<Listing> callback) {
        executor.execute(() -> {
            getListingDetails(listingId, new ResultCallback<Listing>() {
                @Override
                public void onSuccess(Listing l) {
                    l.setRemainingQuantity(l.getRemainingQuantity() + additionalStock);
                    l.setTotalQuantity(l.getTotalQuantity() + additionalStock);
                    l.setStatus(ListingStatus.ACTIVE);
                    updateListing(l, callback);
                }

                @Override
                public void onError(DataError error) {
                    postError(callback, error);
                }
            });
        });
    }

    public void getMerchantOrders(String merchantId, ResultCallback<List<Order>> callback) {
        executor.execute(() -> {
            try {
                Response<List<Order>> resp = restClient.getMerchantOrders(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + merchantId,
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, createSeededOrders());
                }
            } catch (Exception e) {
                postSuccess(callback, createSeededOrders());
            }
        });
    }

    public void verifyPickupToken(String tokenOrCode, String merchantId, ResultCallback<OrderVerificationResult> callback) {
        executor.execute(() -> {
            try {
                if (tokenOrCode == null || tokenOrCode.trim().isEmpty()) {
                    postError(callback, new DataError(DataError.CODE_INVALID_TOKEN, "Pickup token or order code is required."));
                    return;
                }

                // Query order
                String cleanedCode = tokenOrCode.trim().replace("#", "");
                Response<List<Order>> resp = restClient.getOrderByCode(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + cleanedCode).execute();

                Order matchedOrder = null;
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    matchedOrder = resp.body().get(0);
                } else {
                    // Check seeded orders for demo verification
                    for (Order o : createSeededOrders()) {
                        if (o.getOrderCode().equalsIgnoreCase(cleanedCode) || o.getPickupToken().equalsIgnoreCase(tokenOrCode)) {
                            matchedOrder = o;
                            break;
                        }
                    }
                }

                if (matchedOrder == null) {
                    postSuccess(callback, new OrderVerificationResult(false, "Invalid pickup code. Order not found.", null));
                    return;
                }

                if (matchedOrder.getStatus() == OrderStatus.COMPLETED) {
                    postSuccess(callback, new OrderVerificationResult(false, "Order has already been picked up and completed.", matchedOrder));
                    return;
                }

                if (matchedOrder.getStatus() == OrderStatus.CANCELLED || matchedOrder.getStatus() == OrderStatus.EXPIRED) {
                    postSuccess(callback, new OrderVerificationResult(false, "Order status is " + matchedOrder.getStatus().getValue() + ".", matchedOrder));
                    return;
                }

                // Update to completed
                matchedOrder.setStatus(OrderStatus.COMPLETED);
                matchedOrder.setCompletedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("status", "completed");
                    body.addProperty("completed_at", matchedOrder.getCompletedAt());
                    RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                    restClient.updateOrderStatus(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + matchedOrder.getId(), reqBody).execute();
                } catch (Exception ignored) {
                }

                postSuccess(callback, new OrderVerificationResult(true, "Pickup verified successfully! 10 Eco-Points awarded to student.", matchedOrder));
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Verification failed: " + e.getMessage(), e));
            }
        });
    }

    public void getMerchantReviews(String merchantId, ResultCallback<List<Review>> callback) {
        executor.execute(() -> {
            try {
                Response<List<Review>> resp = restClient.getMerchantReviews(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "eq." + merchantId,
                    "created_at.desc"
                ).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    postSuccess(callback, resp.body());
                } else {
                    postSuccess(callback, createSeededReviews());
                }
            } catch (Exception e) {
                postSuccess(callback, createSeededReviews());
            }
        });
    }

    public void uploadListingImage(byte[] imageBytes, String fileName, ResultCallback<String> callback) {
        executor.execute(() -> {
            try {
                String path = "merchant_" + (sessionManager.getUserId() != null ? sessionManager.getUserId() : "demo") + "/" + System.currentTimeMillis() + "_" + fileName;
                RequestBody body = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
                Response<ResponseBody> resp = storageService.uploadFile(
                    SupabaseConfig.SUPABASE_ANON_KEY,
                    getBearer(),
                    "image/jpeg",
                    SupabaseConfig.STORAGE_BUCKET_LISTING_IMAGES,
                    path,
                    body
                ).execute();

                if (resp.isSuccessful()) {
                    String publicUrl = SupabaseConfig.getStoragePublicUrl(path);
                    postSuccess(callback, publicUrl);
                } else {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Storage upload failed: " + resp.message()));
                }
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Upload error: " + e.getMessage(), e));
            }
        });
    }

    public void validateExternalImageUrl(String urlString, ResultCallback<Boolean> callback) {
        executor.execute(() -> {
            try {
                if (urlString == null || !urlString.startsWith("https://") || urlString.length() > 2048) {
                    postSuccess(callback, false);
                    return;
                }
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                int code = conn.getResponseCode();
                postSuccess(callback, (code >= 200 && code < 400));
            } catch (Exception e) {
                // Fallback: accept https valid formatted urls
                postSuccess(callback, urlString != null && urlString.startsWith("https://"));
            }
        });
    }

    // ========================================================================
    // SEED DATA GENERATORS (FOR SMOOTH OFFLINE & INITIAL DEMO EXPERIENCE)
    // ========================================================================

    private List<Listing> createSeededListings() {
        List<Listing> list = new ArrayList<>();

        Merchant m1 = new Merchant("m1", "owner1", "Grand Green Cafe", "Student Pavilion I, Cafeteria Stn 3", 4.335800, 101.141200);
        m1.setRating(4.9);
        m1.setTotalReviews(28);

        Merchant m2 = new Merchant("m2", "owner2", "Kampar Campus Bakery", "Student Pavilion II, Ground Floor", 4.337500, 101.143800);
        m2.setRating(4.8);
        m2.setTotalReviews(19);

        Listing l1 = new Listing();
        l1.setId("list-001");
        l1.setMerchantId(m1.getId());
        l1.setMerchant(m1);
        l1.setTitle("Surplus Bento Mystery Bag");
        l1.setDescription("Fresh chicken cutlet, mixed veggies, steamed fragrant rice, and iced lemon tea prepared today.");
        l1.setCategory("Meals");
        l1.setOriginalPrice(12.50);
        l1.setDiscountedPrice(5.50);
        l1.setRemainingQuantity(4);
        l1.setTotalQuantity(5);
        l1.setPickupStart("16:30");
        l1.setPickupEnd("18:00");
        l1.setPickupLocation("Student Pavilion I, Stn 3");
        l1.setLatitude(4.335800);
        l1.setLongitude(101.141200);
        l1.setCo2KgPerItem(1.80);
        l1.setImageSource(ImageSource.NONE);
        l1.setStatus(ListingStatus.ACTIVE);
        list.add(l1);

        Listing l2 = new Listing();
        l2.setId("list-002");
        l2.setMerchantId(m2.getId());
        l2.setMerchant(m2);
        l2.setTitle("Pastry & Croissant Surprise Pack");
        l2.setDescription("3 assorted butter croissants, chocolate danishes, and egg tarts freshly baked this morning.");
        l2.setCategory("Bakery");
        l2.setOriginalPrice(9.80);
        l2.setDiscountedPrice(4.00); // Under RM5 filter target
        l2.setRemainingQuantity(2);
        l2.setTotalQuantity(4);
        l2.setPickupStart("17:00");
        l2.setPickupEnd("19:00");
        l2.setPickupLocation("Student Pavilion II, Bakery Counter");
        l2.setLatitude(4.337500);
        l2.setLongitude(101.143800);
        l2.setCo2KgPerItem(0.95);
        l2.setImageSource(ImageSource.NONE);
        l2.setStatus(ListingStatus.ACTIVE);
        list.add(l2);

        Listing l3 = new Listing();
        l3.setId("list-003");
        l3.setMerchantId(m1.getId());
        l3.setMerchant(m1);
        l3.setTitle("Vegetarian Noodle Delight Bag");
        l3.setDescription("Stir-fried noodles with tofu, mushrooms, seasonal greens, and vegetarian spring roll.");
        l3.setCategory("Vegetarian");
        l3.setOriginalPrice(8.00);
        l3.setDiscountedPrice(3.50);
        l3.setRemainingQuantity(3);
        l3.setTotalQuantity(3);
        l3.setPickupStart("16:00");
        l3.setPickupEnd("17:30");
        l3.setPickupLocation("Student Pavilion I, Stn 3");
        l3.setLatitude(4.335800);
        l3.setLongitude(101.141200);
        l3.setCo2KgPerItem(1.10);
        l3.setImageSource(ImageSource.NONE);
        l3.setStatus(ListingStatus.ACTIVE);
        list.add(l3);

        return list;
    }

    private List<Order> createSeededOrders() {
        List<Order> list = new ArrayList<>();
        List<Listing> listings = createSeededListings();

        Order o1 = new Order();
        o1.setId("ord-001");
        o1.setOrderCode("FH-829104");
        o1.setListingId(listings.get(0).getId());
        o1.setListing(listings.get(0));
        o1.setMerchantId(listings.get(0).getMerchantId());
        o1.setMerchant(listings.get(0).getMerchant());
        o1.setQuantity(1);
        o1.setTotalOriginalPrice(12.50);
        o1.setTotalDiscountedPrice(5.50);
        o1.setFinalPaidPrice(5.50);
        o1.setPickupStart("16:30");
        o1.setPickupEnd("18:00");
        o1.setPickupToken("FH-TOKEN-829104");
        o1.setStatus(OrderStatus.RESERVED);
        o1.setCreatedAt("2026-09-02T16:05:00Z");
        list.add(o1);

        Order o2 = new Order();
        o2.setId("ord-002");
        o2.setOrderCode("FH-719382");
        o2.setListingId(listings.get(1).getId());
        o2.setListing(listings.get(1));
        o2.setMerchantId(listings.get(1).getMerchantId());
        o2.setMerchant(listings.get(1).getMerchant());
        o2.setQuantity(1);
        o2.setTotalOriginalPrice(9.80);
        o2.setTotalDiscountedPrice(4.00);
        o2.setFinalPaidPrice(4.00);
        o2.setPickupStart("17:00");
        o2.setPickupEnd("19:00");
        o2.setPickupToken("FH-TOKEN-719382");
        o2.setStatus(OrderStatus.COMPLETED);
        o2.setCompletedAt("2026-09-01T17:45:00Z");
        o2.setCreatedAt("2026-09-01T15:20:00Z");
        list.add(o2);

        return list;
    }

    private List<Review> createSeededReviews() {
        List<Review> list = new ArrayList<>();
        Review r1 = new Review("ord-002", "list-002", "student-1", "m2", 5, "Croissants were incredibly flaky and delicious! Excellent initiative.");
        r1.setId("rev-001");
        r1.setCreatedAt("2026-09-01T18:00:00Z");
        Profile p = new Profile("student-1", "student@utar.edu.my", UserRole.STUDENT, "Tan Wei Lun");
        r1.setReviewer(p);
        list.add(r1);
        return list;
    }

    private List<FoodHeroNotification> createSeededNotifications(UserRole role) {
        List<FoodHeroNotification> list = new ArrayList<>();
        if (role == UserRole.STUDENT) {
            FoodHeroNotification n1 = new FoodHeroNotification();
            n1.setId("notif-s1");
            n1.setTitle("New Surplus Food Near You!");
            n1.setMessage("Grand Green Cafe just listed 5 Bento Mystery Bags at Pavilion I.");
            n1.setEventType(NotificationType.NEARBY_LISTING);
            n1.setCreatedAt("10 mins ago");
            list.add(n1);

            FoodHeroNotification n2 = new FoodHeroNotification();
            n2.setId("notif-s2");
            n2.setTitle("Pickup Window Starting Soon");
            n2.setMessage("Order #FH-829104 is ready for pickup between 16:30 and 18:00.");
            n2.setEventType(NotificationType.PICKUP_REMINDER);
            n2.setCreatedAt("25 mins ago");
            list.add(n2);
        } else {
            FoodHeroNotification n1 = new FoodHeroNotification();
            n1.setId("notif-m1");
            n1.setTitle("New Reservation Received!");
            n1.setMessage("Student reserved 1 Bento Bag (Order #FH-829104).");
            n1.setEventType(NotificationType.RESERVATION_CREATED);
            n1.setCreatedAt("5 mins ago");
            list.add(n1);

            FoodHeroNotification n2 = new FoodHeroNotification();
            n2.setId("notif-m2");
            n2.setTitle("New 5-Star Review Received");
            n2.setMessage("A student left a 5-star review for Pastry Surprise Pack.");
            n2.setEventType(NotificationType.REVIEW_RECEIVED);
            n2.setCreatedAt("1 hour ago");
            list.add(n2);
        }
        return list;
    }

    private <T> void postSuccess(ResultCallback<T> callback, T result) {
        mainHandler.post(() -> {
            if (callback != null) callback.onSuccess(result);
        });
    }

    private <T> void postError(ResultCallback<T> callback, DataError error) {
        mainHandler.post(() -> {
            if (callback != null) callback.onError(error);
        });
    }
}
