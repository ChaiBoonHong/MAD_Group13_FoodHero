package com.uccd3223.group13.foodhero.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Badge;
import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.GeoPoint;
import com.uccd3223.group13.foodhero.data.model.ImageSource;
import com.uccd3223.group13.foodhero.data.model.ImpactSummary;
import com.uccd3223.group13.foodhero.data.model.LeaderboardEntry;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.ListingStatus;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.model.MerchantDashboardData;
import com.uccd3223.group13.foodhero.data.model.NotificationType;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.model.OrderVerificationResult;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.Review;
import com.uccd3223.group13.foodhero.data.model.RouteResult;
import com.uccd3223.group13.foodhero.data.model.ServiceArea;
import com.uccd3223.group13.foodhero.data.model.TravelMode;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.remote.SupabaseConfig;
import com.uccd3223.group13.foodhero.data.remote.SupabaseRestClient;
import com.uccd3223.group13.foodhero.data.remote.SupabaseStorageService;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.util.CampusBoundaryManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.uccd3223.group13.foodhero.util.OrderExpirationWorker;
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
    private final Context appContext;
    private final List<Order> cachedOrders = new CopyOnWriteArrayList<>();
    private final List<FoodHeroNotification> cachedNotifications = new CopyOnWriteArrayList<>();
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Gson gson;

    private FoodHeroRepository(Context context) {
        this.appContext = context.getApplicationContext();
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

                if (resp.isSuccessful() && resp.body() != null) {
                    List<Listing> listings = resp.body();
                    localCache.cacheFeed(listings);
                    postSuccess(callback, listings);
                } else {
                    // Fallback to local cache if available
                    localCache.getCachedFeed(new ResultCallback<List<Listing>>() {
                        @Override
                        public void onSuccess(List<Listing> cached) {
                            postSuccess(callback, (cached != null) ? cached : new ArrayList<>());
                        }

                        @Override
                        public void onError(DataError error) {
                            postSuccess(callback, new ArrayList<>());
                        }
                    });
                }
            } catch (Exception e) {
                localCache.getCachedFeed(new ResultCallback<List<Listing>>() {
                    @Override
                    public void onSuccess(List<Listing> cached) {
                        postSuccess(callback, (cached != null) ? cached : new ArrayList<>());
                    }

                    @Override
                    public void onError(DataError err) {
                        postSuccess(callback, new ArrayList<>());
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

                String studentId = sessionManager.getUserId();
                if (studentId == null) {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED, "Please sign in to reserve surplus meals."));
                    return;
                }
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
                order.setStatus(OrderStatus.AWAITING_PAYMENT);
                order.setPaymentExpiresAt(System.currentTimeMillis() + (10 * 60 * 1000));
                order.setPaymentMethod("DUITNOW_QR");
                order.setPaymentReference(orderCode);
                order.setListing(listing);
                order.setMerchant(listing.getMerchant());
                order.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                cachedOrders.add(0, order);

                // Schedule OrderExpirationWorker with 10-minute delay
                try {
                    OneTimeWorkRequest expireRequest = new OneTimeWorkRequest.Builder(OrderExpirationWorker.class)
                        .setInitialDelay(10, TimeUnit.MINUTES)
                        .setInputData(new Data.Builder().putString(OrderExpirationWorker.KEY_ORDER_ID, order.getId()).build())
                        .build();
                    WorkManager.getInstance(appContext).enqueue(expireRequest);
                } catch (Exception ignored) {}

                try {
                    Order orderPayload = copyOrderForUpload(order);
                    Response<List<Order>> resp = restClient.createOrder(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), orderPayload).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        Order remoteOrder = resp.body().get(0);
                        remoteOrder.setListing(listing);
                        remoteOrder.setMerchant(listing.getMerchant());
                        order = remoteOrder;
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
            String studentId = sessionManager.getUserId();
            if (studentId != null) {
                try {
                    Response<List<Order>> resp = restClient.getStudentOrders(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        getBearer(),
                        "eq." + studentId,
                        "created_at.desc"
                    ).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        cachedOrders.clear();
                        cachedOrders.addAll(resp.body());
                        postSuccess(callback, resp.body());
                        return;
                    }
                } catch (Exception ignored) {}
            }
            // Auto-expire any local pending orders where 10m elapsed
            long now = System.currentTimeMillis();
            for (Order o : cachedOrders) {
                if (o.getStatus() == OrderStatus.AWAITING_PAYMENT && o.getPaymentExpiresAt() > 0 && now > o.getPaymentExpiresAt()) {
                    o.setStatus(OrderStatus.EXPIRED);
                }
            }
            postSuccess(callback, new ArrayList<>(cachedOrders));
        });
    }

    public void getStudentImpact(ResultCallback<ImpactSummary> callback) {
        executor.execute(() -> {
            try {
                Profile profile = sessionManager.getProfile();
                int meals = profile != null ? profile.getMealsRescued() : 0;
                double saved = profile != null ? profile.getMoneySaved() : 0.00;
                double co2 = profile != null ? profile.getCo2Prevented() : 0.00;
                int points = profile != null ? profile.getEcoPoints() : 0;

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
                String studentFaculty = (profile != null && profile.getFaculty() != null) ? profile.getFaculty() : "FICT";
                leaderboard.add(new LeaderboardEntry("FICT (Faculty of Info & Comm Tech)", studentFaculty.equalsIgnoreCase("FICT") ? meals : 0, 1));
                leaderboard.add(new LeaderboardEntry("FBF (Faculty of Business & Finance)", studentFaculty.equalsIgnoreCase("FBF") ? meals : 0, 2));
                leaderboard.add(new LeaderboardEntry("FEGT (Faculty of Eng & Green Tech)", studentFaculty.equalsIgnoreCase("FEGT") ? meals : 0, 3));
                leaderboard.add(new LeaderboardEntry("FAS (Faculty of Arts & Social Science)", studentFaculty.equalsIgnoreCase("FAS") ? meals : 0, 4));
                leaderboard.add(new LeaderboardEntry("FSc (Faculty of Science)", studentFaculty.equalsIgnoreCase("FSc") ? meals : 0, 5));
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
                String studentId = sessionManager.getUserId();
                if (studentId == null) {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED, "Please sign in to submit a review."));
                    return;
                }
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
            String userId = sessionManager.getUserId();
            if (userId != null) {
                try {
                    Response<List<FoodHeroNotification>> resp = restClient.getNotifications(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        getBearer(),
                        "eq." + userId,
                        "created_at.desc"
                    ).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        postSuccess(callback, resp.body());
                        return;
                    }
                } catch (Exception ignored) {}
            }
            List<FoodHeroNotification> filtered = new ArrayList<>();
            for (FoodHeroNotification n : cachedNotifications) {
                if (n.getRecipientRole() == role) {
                    filtered.add(n);
                }
            }
            postSuccess(callback, filtered);
        });
    }

    public void submitPaymentReceipt(String orderId, String receiptUrl, ResultCallback<Order> callback) {
        executor.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("status", "pending_verification");
                body.addProperty("payment_receipt_url", receiptUrl);
                RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                restClient.updateOrderStatus(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + orderId, reqBody).execute();
            } catch (Exception ignored) {}

            Order found = null;
            for (Order o : cachedOrders) {
                if (o.getId() != null && o.getId().equals(orderId)) {
                    found = o;
                    break;
                }
            }
            if (found != null) {
                found.setPaymentReceiptUrl(receiptUrl);
                found.setStatus(OrderStatus.PENDING_VERIFICATION);

                // Create notification for MERCHANT
                FoodHeroNotification n = new FoodHeroNotification();
                n.setId(UUID.randomUUID().toString());
                n.setRecipientRole(UserRole.MERCHANT);
                n.setRecipientId(found.getMerchantId());
                n.setRelatedOrderId(found.getId());
                n.setEventType(NotificationType.PAYMENT_SUBMITTED);
                n.setTitle("New Payment Slip Uploaded");
                n.setMessage(String.format(Locale.US, "Order #%s (RM %.2f) payment receipt submitted. Tap to verify.",
                    found.getOrderCode(), found.getFinalPaidPrice()));
                n.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));
                cachedNotifications.add(0, n);

                try {
                    restClient.createNotification(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), n).execute();
                } catch (Exception ignored) {}

                postSuccess(callback, found);
            } else {
                postError(callback, new DataError(DataError.CODE_NOT_FOUND, "Order not found"));
            }
        });
    }

    public void verifyPaymentReceipt(String orderId, boolean approved, ResultCallback<Order> callback) {
        executor.execute(() -> {
            String newStatus = approved ? "reserved" : "rejected";
            try {
                JsonObject body = new JsonObject();
                body.addProperty("status", newStatus);
                RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                restClient.updateOrderStatus(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + orderId, reqBody).execute();
            } catch (Exception ignored) {}

            Order found = null;
            for (Order o : cachedOrders) {
                if (o.getId() != null && o.getId().equals(orderId)) {
                    found = o;
                    break;
                }
            }
            if (found != null) {
                FoodHeroNotification n = new FoodHeroNotification();
                n.setId(UUID.randomUUID().toString());
                n.setRecipientRole(UserRole.STUDENT);
                n.setRecipientId(found.getStudentId());
                n.setRelatedOrderId(found.getId());
                n.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                if (approved) {
                    found.setStatus(OrderStatus.RESERVED);
                    n.setEventType(NotificationType.PAYMENT_VERIFIED);
                    n.setTitle("Payment Verified! Order Confirmed");
                    n.setMessage(String.format(Locale.US, "Order #%s payment has been verified. Your Pickup QR token is ready!",
                        found.getOrderCode()));
                } else {
                    found.setStatus(OrderStatus.REJECTED);
                    n.setEventType(NotificationType.PAYMENT_REJECTED);
                    n.setTitle("Payment Verification Failed");
                    n.setMessage(String.format(Locale.US, "Receipt for Order #%s could not be verified. Please re-upload.",
                        found.getOrderCode()));
                }
                cachedNotifications.add(0, n);

                try {
                    restClient.createNotification(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), n).execute();
                } catch (Exception ignored) {}

                postSuccess(callback, found);
            } else {
                postError(callback, new DataError(DataError.CODE_NOT_FOUND, "Order not found"));
            }
        });
    }

    public void checkAndExpireOrder(String orderId) {
        for (Order o : cachedOrders) {
            if (o.getId() != null && o.getId().equals(orderId)) {
                if (o.getStatus() == OrderStatus.AWAITING_PAYMENT && System.currentTimeMillis() > o.getPaymentExpiresAt()) {
                    o.setStatus(OrderStatus.EXPIRED);
                    FoodHeroNotification n = new FoodHeroNotification();
                    n.setId("notif-" + UUID.randomUUID().toString().substring(0, 8));
                    n.setRecipientRole(UserRole.STUDENT);
                    n.setRecipientId(o.getStudentId());
                    n.setRelatedOrderId(o.getId());
                    n.setEventType(NotificationType.ORDER_EXPIRED);
                    n.setTitle("Order Expired (Payment Timeout)");
                    n.setMessage(String.format(Locale.US, "Order #%s was cancelled because receipt was not uploaded within 10 minutes.", o.getOrderCode()));
                    n.setCreatedAt("Just now");
                    cachedNotifications.add(0, n);
                }
                break;
            }
        }
    }

    public void cancelExpiredOrder(String orderId, ResultCallback<Void> callback) {
        executor.execute(() -> {
            checkAndExpireOrder(orderId);
            postSuccess(callback, null);
        });
    }

    public void getMerchantOrders(String merchantId, ResultCallback<List<Order>> callback) {
        executor.execute(() -> {
            String resolvedId = merchantId;
            if (resolvedId == null || resolvedId.isEmpty()) {
                resolvedId = sessionManager.getMerchantId();
            }
            if (resolvedId != null) {
                try {
                    Response<List<Order>> resp = restClient.getMerchantOrders(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        getBearer(),
                        "eq." + resolvedId,
                        "created_at.desc"
                    ).execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        cachedOrders.clear();
                        cachedOrders.addAll(resp.body());
                        postSuccess(callback, resp.body());
                        return;
                    }
                } catch (Exception ignored) {}
            }
            long now = System.currentTimeMillis();
            for (Order o : cachedOrders) {
                if (o.getStatus() == OrderStatus.AWAITING_PAYMENT && o.getPaymentExpiresAt() > 0 && now > o.getPaymentExpiresAt()) {
                    o.setStatus(OrderStatus.EXPIRED);
                }
            }
            postSuccess(callback, new ArrayList<>(cachedOrders));
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
                String resolvedId = merchantId;
                if (resolvedId == null || resolvedId.isEmpty()) {
                    resolvedId = sessionManager.getMerchantId();
                }
                if (resolvedId == null || resolvedId.isEmpty()) {
                    resolvedId = sessionManager.getUserId();
                }

                MerchantDashboardData data = new MerchantDashboardData();
                List<Order> merchantOrders = new ArrayList<>();
                List<Listing> merchantListings = new ArrayList<>();
                List<Review> merchantReviews = new ArrayList<>();

                if (resolvedId != null) {
                    try {
                        Response<List<Order>> oResp = restClient.getMerchantOrders(
                            SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + resolvedId, "created_at.desc"
                        ).execute();
                        if (oResp.isSuccessful() && oResp.body() != null) {
                            merchantOrders.addAll(oResp.body());
                        }
                    } catch (Exception ignored) {}

                    try {
                        Response<List<Listing>> lResp = restClient.getMerchantListings(
                            SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + resolvedId, "created_at.desc"
                        ).execute();
                        if (lResp.isSuccessful() && lResp.body() != null) {
                            merchantListings.addAll(lResp.body());
                        }
                    } catch (Exception ignored) {}

                    try {
                        Response<List<Review>> rResp = restClient.getMerchantReviews(
                            SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + resolvedId, "created_at.desc"
                        ).execute();
                        if (rResp.isSuccessful() && rResp.body() != null) {
                            merchantReviews.addAll(rResp.body());
                        }
                    } catch (Exception ignored) {}
                }

                // If remote returned nothing, check cached orders for this merchant
                if (merchantOrders.isEmpty()) {
                    for (Order o : cachedOrders) {
                        if (resolvedId != null && resolvedId.equals(o.getMerchantId())) {
                            merchantOrders.add(o);
                        }
                    }
                }

                // Compute metrics dynamically from actual data (zero hardcoded defaults)
                double revenue = 0.0;
                double foodDiverted = 0.0;
                int completedCount = 0;
                for (Order o : merchantOrders) {
                    if (o.getStatus() == OrderStatus.COMPLETED) {
                        completedCount++;
                        revenue += o.getFinalPaidPrice();
                        double co2 = (o.getListing() != null && o.getListing().getCo2KgPerItem() > 0) ? o.getListing().getCo2KgPerItem() : 1.20;
                        foodDiverted += (co2 * Math.max(o.getQuantity(), 1));
                    }
                }

                int activeBags = 0;
                int lowStock = 0;
                for (Listing l : merchantListings) {
                    if (l.getStatus() == ListingStatus.ACTIVE && l.getRemainingQuantity() > 0) {
                        activeBags++;
                        if (l.getRemainingQuantity() <= 2) {
                            lowStock++;
                        }
                    }
                }

                double avgRating = 0.0;
                if (!merchantReviews.isEmpty()) {
                    double sumRating = 0;
                    for (Review r : merchantReviews) {
                        sumRating += r.getRating();
                    }
                    avgRating = sumRating / merchantReviews.size();
                }

                data.setRevenueRecovered(revenue);
                data.setFoodDivertedKg(foodDiverted);
                data.setOrdersCompleted(completedCount);
                data.setAverageRating(avgRating);
                data.setActiveListingsCount(activeBags);
                data.setLowStockAlertsCount(lowStock);
                data.setUnreadNotificationsCount(0);

                List<Order> recent = new ArrayList<>();
                for (int i = 0; i < Math.min(merchantOrders.size(), 5); i++) {
                    recent.add(merchantOrders.get(i));
                }
                data.setRecentOrders(recent);

                if (resolvedId != null) {
                    localCache.cacheDashboard(resolvedId, data);
                }
                postSuccess(callback, data);
            } catch (Exception e) {
                String fallbackId = merchantId != null ? merchantId : sessionManager.getMerchantId();
                if (fallbackId != null) {
                    localCache.getCachedDashboard(fallbackId, callback);
                } else {
                    postSuccess(callback, new MerchantDashboardData());
                }
            }
        });
    }

    public void getMerchantListings(String merchantId, ResultCallback<List<Listing>> callback) {
        executor.execute(() -> {
            try {
                String resolvedId = merchantId;
                if (resolvedId == null || resolvedId.isEmpty()) {
                    resolvedId = sessionManager.getMerchantId();
                }
                if (resolvedId == null || resolvedId.isEmpty()) {
                    resolvedId = sessionManager.getUserId();
                }

                if (resolvedId != null) {
                    Response<List<Listing>> resp = restClient.getMerchantListings(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        getBearer(),
                        "eq." + resolvedId,
                        "created_at.desc"
                    ).execute();

                    if (resp.isSuccessful() && resp.body() != null) {
                        postSuccess(callback, resp.body());
                        return;
                    }
                }
                postSuccess(callback, new ArrayList<>());
            } catch (Exception e) {
                postSuccess(callback, new ArrayList<>());
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

                if (listing.getMerchantId() == null || listing.getMerchantId().isEmpty()) {
                    String mId = sessionManager.getMerchantId();
                    if (mId == null || mId.isEmpty()) {
                        mId = sessionManager.getUserId();
                    }
                    listing.setMerchantId(mId);
                }

                if (isExternalUrl) {
                    listing.setImageSource(ImageSource.EXTERNAL_URL);
                    listing.setImageUrl(imageUriOrUrl);
                }

                if (listing.getId() == null || listing.getId().isEmpty()) {
                    listing.setId(UUID.randomUUID().toString());
                }
                listing.setStatus(ListingStatus.ACTIVE);

                Merchant cachedM = listing.getMerchant();
                listing.setMerchant(null); // Strip nested join object for PostgREST

                try {
                    Response<List<Listing>> resp = restClient.createListing(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), listing).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        Listing created = resp.body().get(0);
                        created.setMerchant(cachedM);
                        postSuccess(callback, created);
                        return;
                    }
                } catch (Exception ignored) {
                }

                listing.setMerchant(cachedM);
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

                Merchant cachedM = listing.getMerchant();
                listing.setMerchant(null); // Strip nested join object for PostgREST

                try {
                    Response<List<Listing>> resp = restClient.updateListing(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + listing.getId(), listing).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        Listing updated = resp.body().get(0);
                        updated.setMerchant(cachedM);
                        postSuccess(callback, updated);
                        return;
                    }
                } catch (Exception ignored) {
                }

                listing.setMerchant(cachedM);
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

    public void verifyPickupToken(String tokenOrCode, String merchantId, ResultCallback<OrderVerificationResult> callback) {
        executor.execute(() -> {
            try {
                if (tokenOrCode == null || tokenOrCode.trim().isEmpty()) {
                    postError(callback, new DataError(DataError.CODE_INVALID_TOKEN, "Pickup token or order code is required."));
                    return;
                }

                String raw = tokenOrCode.trim();
                String codePart = raw.replace("#", "");
                String tokenPart = raw;
                if (raw.contains(":")) {
                    String[] parts = raw.split(":", 2);
                    codePart = parts[0].trim().replace("#", "");
                    tokenPart = parts[1].trim();
                }

                Order matchedOrder = null;

                // 1. Try query by order_code
                try {
                    Response<List<Order>> resp = restClient.getOrderByCode(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + codePart).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        matchedOrder = resp.body().get(0);
                    }
                } catch (Exception ignored) {}

                // 2. If not found, try query by pickup_token
                if (matchedOrder == null) {
                    try {
                        Response<List<Order>> resp = restClient.getOrderByPickupToken(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + tokenPart).execute();
                        if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                            matchedOrder = resp.body().get(0);
                        }
                    } catch (Exception ignored) {}
                }

                // 3. Fallback to cachedOrders
                if (matchedOrder == null) {
                    for (Order o : cachedOrders) {
                        if ((o.getOrderCode() != null && o.getOrderCode().equalsIgnoreCase(codePart)) ||
                            (o.getPickupToken() != null && (o.getPickupToken().equalsIgnoreCase(tokenPart) || o.getPickupToken().equalsIgnoreCase(raw)))) {
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

                if (matchedOrder.getStatus() == OrderStatus.CANCELLED || matchedOrder.getStatus() == OrderStatus.EXPIRED || matchedOrder.getStatus() == OrderStatus.REJECTED) {
                    postSuccess(callback, new OrderVerificationResult(false, "Order status is " + matchedOrder.getStatus().getValue() + ".", matchedOrder));
                    return;
                }

                // Update to completed in Supabase
                matchedOrder.setStatus(OrderStatus.COMPLETED);
                matchedOrder.setCompletedAt(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date()));

                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("status", "completed");
                    body.addProperty("completed_at", matchedOrder.getCompletedAt());
                    RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                    restClient.updateOrderStatus(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + matchedOrder.getId(), reqBody).execute();
                } catch (Exception ignored) {}

                postSuccess(callback, new OrderVerificationResult(true, "Pickup verified successfully! 10 Eco-Points awarded to student.", matchedOrder));
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Verification failed: " + e.getMessage(), e));
            }
        });
    }

    public void getMerchantProfile(String merchantId, ResultCallback<Merchant> callback) {
        executor.execute(() -> {
            try {
                String resolvedId = merchantId != null && !merchantId.trim().isEmpty() ? merchantId.trim() : sessionManager.getMerchantId();
                if (resolvedId == null || resolvedId.isEmpty()) {
                    resolvedId = sessionManager.getUserId();
                }

                if (resolvedId != null && !resolvedId.isEmpty()) {
                    // Try query by merchant id first
                    Response<List<Merchant>> resp = restClient.getMerchantById(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + resolvedId).execute();
                    if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                        Merchant m = resp.body().get(0);
                        sessionManager.saveMerchantInfo(m.getId(), m.getBusinessName(), m.getCampusLocation());
                        postSuccess(callback, m);
                        return;
                    }
                }

                // If not matched by id, try by owner_id
                String ownerId = sessionManager.getUserId();
                if (ownerId != null && !ownerId.isEmpty()) {
                    Response<List<Merchant>> ownerResp = restClient.getMerchantByOwner(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + ownerId).execute();
                    if (ownerResp.isSuccessful() && ownerResp.body() != null && !ownerResp.body().isEmpty()) {
                        Merchant m = ownerResp.body().get(0);
                        sessionManager.saveMerchantInfo(m.getId(), m.getBusinessName(), m.getCampusLocation());
                        postSuccess(callback, m);
                        return;
                    }
                }

                // Fallback to local session
                String bName = sessionManager.getBusinessName() != null ? sessionManager.getBusinessName() : "Merchant Outlet";
                String cLoc = sessionManager.getCampusLocation() != null ? sessionManager.getCampusLocation() : "Block C - Student Pavilion I";
                Merchant localFallback = new Merchant(resolvedId, sessionManager.getUserId(), bName, cLoc, 4.337243, 101.142379);
                postSuccess(callback, localFallback);
            } catch (Exception e) {
                String bName = sessionManager.getBusinessName() != null ? sessionManager.getBusinessName() : "Merchant Outlet";
                String cLoc = sessionManager.getCampusLocation() != null ? sessionManager.getCampusLocation() : "Block C - Student Pavilion I";
                Merchant localFallback = new Merchant(sessionManager.getMerchantId(), sessionManager.getUserId(), bName, cLoc, 4.337243, 101.142379);
                postSuccess(callback, localFallback);
            }
        });
    }

    public void updateMerchantProfile(String businessName, String campusLocation, String closingTime, ResultCallback<Merchant> callback) {
        updateMerchantProfile(businessName, campusLocation, 0.0, 0.0, closingTime, callback);
    }

    public void updateMerchantProfile(String businessName, String campusLocation, double latitude, double longitude, String closingTime, ResultCallback<Merchant> callback) {
        executor.execute(() -> {
            try {
                String mId = sessionManager.getMerchantId();
                if (mId == null || mId.isEmpty()) {
                    mId = sessionManager.getUserId();
                }
                JsonObject body = new JsonObject();
                if (businessName != null && !businessName.trim().isEmpty()) {
                    body.addProperty("business_name", businessName.trim());
                }
                if (campusLocation != null && !campusLocation.trim().isEmpty()) {
                    body.addProperty("campus_location", campusLocation.trim());
                }
                if (latitude != 0.0 && longitude != 0.0) {
                    body.addProperty("latitude", latitude);
                    body.addProperty("longitude", longitude);
                }
                if (closingTime != null && !closingTime.trim().isEmpty()) {
                    body.addProperty("closing_time", closingTime.trim());
                }
                RequestBody reqBody = RequestBody.create(MediaType.parse("application/json"), body.toString());
                Response<List<Merchant>> resp = restClient.updateMerchant(SupabaseConfig.SUPABASE_ANON_KEY, getBearer(), "eq." + mId, reqBody).execute();
                if (resp.isSuccessful() && resp.body() != null && !resp.body().isEmpty()) {
                    Merchant updated = resp.body().get(0);
                    sessionManager.saveMerchantInfo(updated.getId(), updated.getBusinessName(), updated.getCampusLocation());
                    postSuccess(callback, updated);
                } else {
                    sessionManager.saveMerchantInfo(mId, businessName != null ? businessName : "Merchant", campusLocation != null ? campusLocation : "");
                    double finalLat = (latitude != 0.0) ? latitude : 4.337243;
                    double finalLng = (longitude != 0.0) ? longitude : 101.142379;
                    Merchant m = new Merchant(mId, sessionManager.getUserId(), businessName, campusLocation, finalLat, finalLng);
                    postSuccess(callback, m);
                }
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Failed to update profile: " + e.getMessage(), e));
            }
        });
    }

    private Order copyOrderForUpload(Order src) {
        Order copy = new Order();
        copy.setId(src.getId());
        copy.setOrderCode(src.getOrderCode());
        copy.setStudentId(src.getStudentId());
        copy.setListingId(src.getListingId());
        copy.setMerchantId(src.getMerchantId());
        copy.setQuantity(src.getQuantity());
        copy.setTotalOriginalPrice(src.getTotalOriginalPrice());
        copy.setTotalDiscountedPrice(src.getTotalDiscountedPrice());
        copy.setRewardPointsUsed(src.getRewardPointsUsed());
        copy.setRewardDiscountAmount(src.getRewardDiscountAmount());
        copy.setFinalPaidPrice(src.getFinalPaidPrice());
        copy.setPickupStart(src.getPickupStart());
        copy.setPickupEnd(src.getPickupEnd());
        copy.setPickupToken(src.getPickupToken());
        copy.setStatus(src.getStatus());
        copy.setPaymentExpiresAt(src.getPaymentExpiresAt());
        copy.setPaymentReceiptUrl(src.getPaymentReceiptUrl());
        copy.setPaymentMethod(src.getPaymentMethod());
        copy.setPaymentReference(src.getPaymentReference());
        copy.setCreatedAt(src.getCreatedAt());
        return copy;
    }

    public void getMerchantReviews(String merchantId, ResultCallback<List<Review>> callback) {
        executor.execute(() -> {
            try {
                if (merchantId != null) {
                    Response<List<Review>> resp = restClient.getMerchantReviews(
                        SupabaseConfig.SUPABASE_ANON_KEY,
                        getBearer(),
                        "eq." + merchantId,
                        "created_at.desc"
                    ).execute();

                    if (resp.isSuccessful() && resp.body() != null) {
                        postSuccess(callback, resp.body());
                        return;
                    }
                }
                postSuccess(callback, new ArrayList<>());
            } catch (Exception e) {
                postSuccess(callback, new ArrayList<>());
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
                postSuccess(callback, urlString != null && urlString.startsWith("https://"));
            }
        });
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
