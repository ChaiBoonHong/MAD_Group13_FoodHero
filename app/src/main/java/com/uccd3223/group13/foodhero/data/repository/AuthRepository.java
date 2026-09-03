package com.uccd3223.group13.foodhero.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.remote.AuthIdTokenRequest;
import com.uccd3223.group13.foodhero.data.remote.AuthRequest;
import com.uccd3223.group13.foodhero.data.remote.AuthResponse;
import com.uccd3223.group13.foodhero.data.remote.SupabaseAuthService;
import com.uccd3223.group13.foodhero.data.remote.SupabaseConfig;
import com.uccd3223.group13.foodhero.data.remote.SupabaseRestClient;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AuthRepository {
    private static volatile AuthRepository INSTANCE;
    private final SupabaseAuthService authService;
    private final SupabaseRestClient restClient;
    private final SessionManager sessionManager;
    private final ExecutorService executor;
    private final Handler mainHandler;

    private AuthRepository(Context context) {
        this.sessionManager = SessionManager.getInstance(context);
        this.executor = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(logging)
            .build();

        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl(SupabaseConfig.SUPABASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(new Gson()))
            .build();

        this.authService = retrofit.create(SupabaseAuthService.class);
        this.restClient = retrofit.create(SupabaseRestClient.class);
    }

    public static AuthRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AuthRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AuthRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    public boolean isLoggedIn() {
        return sessionManager.isLoggedIn();
    }

    public Profile getCurrentProfile() {
        return sessionManager.getProfile();
    }

    public UserRole getCurrentUserRole() {
        return sessionManager.getUserRole();
    }

    public void register(
        String email,
        String password,
        UserRole role,
        String fullName,
        String studentId,
        String faculty,
        String businessName,
        String campusLocation,
        ResultCallback<Profile> callback
    ) {
        executor.execute(() -> {
            try {
                java.util.Map<String, Object> metaData = new java.util.HashMap<>();
                metaData.put("role", role != null ? role.name().toLowerCase() : "student");
                metaData.put("full_name", fullName);
                if (studentId != null && !studentId.isEmpty()) metaData.put("student_id", studentId);
                if (faculty != null && !faculty.isEmpty()) metaData.put("faculty", faculty);
                if (businessName != null && !businessName.isEmpty()) metaData.put("business_name", businessName);
                if (campusLocation != null && !campusLocation.isEmpty()) metaData.put("campus_location", campusLocation);

                AuthRequest req = new AuthRequest(email, password, metaData);
                Response<AuthResponse> resp = authService.signUp(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();

                if (!resp.isSuccessful() || resp.body() == null || resp.body().getUser() == null) {
                    String errorMsg = "Registration failed";
                    if (resp.errorBody() != null) {
                        try {
                            String errStr = resp.errorBody().string();
                            com.google.gson.JsonObject errObj = new Gson().fromJson(errStr, com.google.gson.JsonObject.class);
                            if (errObj != null) {
                                if (errObj.has("msg")) errorMsg = errObj.get("msg").getAsString();
                                else if (errObj.has("message")) errorMsg = errObj.get("message").getAsString();
                                else if (errObj.has("error_description")) errorMsg = errObj.get("error_description").getAsString();
                            }
                        } catch (Exception ignored) {}
                    }
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, errorMsg));
                    return;
                }

                String userId = resp.body().getUser().getId();
                String accessToken = resp.body().getAccessToken() != null ? resp.body().getAccessToken() : SupabaseConfig.SUPABASE_ANON_KEY;
                String refreshToken = resp.body().getRefreshToken() != null ? resp.body().getRefreshToken() : "";

                // Upsert Profile in database
                Profile profile = new Profile(userId, email, role, fullName);
                profile.setStudentId(studentId);
                profile.setFaculty(faculty);

                String bearer = "Bearer " + accessToken;
                restClient.upsertProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, profile).execute();

                if (role == UserRole.MERCHANT) {
                    ensureMerchantLoaded(userId, bearer, businessName, campusLocation);
                }

                sessionManager.saveSession(accessToken, refreshToken, profile);
                postSuccess(callback, profile);

            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error during registration: " + e.getMessage(), e));
            }
        });
    }

    public void login(String email, String password, ResultCallback<Profile> callback) {
        executor.execute(() -> {
            try {
                AuthRequest req = new AuthRequest(email, password);
                Response<AuthResponse> resp = authService.signInWithPassword(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();

                if (!resp.isSuccessful() || resp.body() == null || resp.body().getUser() == null) {
                    String errorMsg = "Invalid email or password.";
                    if (resp.errorBody() != null) {
                        try {
                            String errStr = resp.errorBody().string();
                            com.google.gson.JsonObject errObj = new Gson().fromJson(errStr, com.google.gson.JsonObject.class);
                            if (errObj != null) {
                                if (errObj.has("msg")) errorMsg = errObj.get("msg").getAsString();
                                else if (errObj.has("message")) errorMsg = errObj.get("message").getAsString();
                                else if (errObj.has("error_description")) errorMsg = errObj.get("error_description").getAsString();
                            }
                        } catch (Exception ignored) {}
                    }
                    postError(callback, new DataError(DataError.CODE_INVALID_CREDENTIALS, errorMsg));
                    return;
                }

                String userId = resp.body().getUser().getId();
                String accessToken = resp.body().getAccessToken();
                String refreshToken = resp.body().getRefreshToken();
                String bearer = "Bearer " + accessToken;

                // Fetch Profile from database
                Response<List<Profile>> profileResp = restClient.getProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();

                Profile profile;
                if (profileResp.isSuccessful() && profileResp.body() != null && !profileResp.body().isEmpty()) {
                    profile = profileResp.body().get(0);
                } else {
                    // Fallback profile if profile row is pending trigger
                    UserRole fallbackRole = (email != null && email.toLowerCase().contains("merchant")) ? UserRole.MERCHANT : UserRole.STUDENT;
                    profile = new Profile(userId, email, fallbackRole, email != null && email.contains("@") ? email.split("@")[0] : "User");
                }

                // If merchant, load or initialize their merchant outlet record
                if (profile.getRole() == UserRole.MERCHANT) {
                    ensureMerchantLoaded(userId, bearer, profile.getFullName(), "Student Pavilion I");
                }

                sessionManager.saveSession(accessToken, refreshToken, profile);
                postSuccess(callback, profile);

            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error during login: " + e.getMessage(), e));
            }
        });
    }

    public void signInWithGoogle(String idToken, UserRole role, ResultCallback<Profile> callback) {
        executor.execute(() -> {
            try {
                AuthIdTokenRequest req = new AuthIdTokenRequest("google", idToken);
                Response<AuthResponse> resp = authService.signInWithIdToken(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();

                if (!resp.isSuccessful() || resp.body() == null || resp.body().getUser() == null) {
                    String errorMsg = "Google authentication failed";
                    if (resp.errorBody() != null) {
                        try {
                            errorMsg = resp.errorBody().string();
                        } catch (Exception ignored) {}
                    }
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, errorMsg));
                    return;
                }

                String userId = resp.body().getUser().getId();
                String email = resp.body().getUser().getEmail();
                String accessToken = resp.body().getAccessToken() != null ? resp.body().getAccessToken() : SupabaseConfig.SUPABASE_ANON_KEY;
                String refreshToken = resp.body().getRefreshToken() != null ? resp.body().getRefreshToken() : "";
                String bearer = "Bearer " + accessToken;

                // Check or upsert profile in Supabase
                Response<List<Profile>> profileResp = restClient.getProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();
                Profile profile;
                if (profileResp.isSuccessful() && profileResp.body() != null && !profileResp.body().isEmpty()) {
                    profile = profileResp.body().get(0);
                } else {
                    String name = (email != null && email.contains("@")) ? email.split("@")[0] : "Eco Hero";
                    profile = new Profile(userId, email, role != null ? role : UserRole.STUDENT, name);
                    try {
                        restClient.upsertProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, profile).execute();
                    } catch (Exception e) {
                        android.util.Log.w("AuthRepository", "Profile upsert note: " + e.getMessage());
                    }
                }

                if (profile.getRole() == UserRole.MERCHANT) {
                    ensureMerchantLoaded(userId, bearer, profile.getFullName(), "Student Pavilion I");
                }

                sessionManager.saveSession(accessToken, refreshToken, profile);
                postSuccess(callback, profile);

            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error during Google Sign-In: " + e.getMessage(), e));
            }
        });
    }

    public void handleOAuthToken(String accessToken, String refreshToken, UserRole role, ResultCallback<Profile> callback) {
        executor.execute(() -> {
            try {
                String bearer = "Bearer " + accessToken;
                Response<AuthResponse.SupabaseUser> userResp = authService.getUser(SupabaseConfig.SUPABASE_ANON_KEY, bearer).execute();
                if (!userResp.isSuccessful() || userResp.body() == null) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Unable to load user with OAuth token"));
                    return;
                }
                String userId = userResp.body().getId();
                String email = userResp.body().getEmail();

                Response<List<Profile>> profileResp = restClient.getProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();
                Profile profile;
                if (profileResp.isSuccessful() && profileResp.body() != null && !profileResp.body().isEmpty()) {
                    profile = profileResp.body().get(0);
                } else {
                    String name = (email != null && email.contains("@")) ? email.split("@")[0] : "FoodHero User";
                    profile = new Profile(userId, email, role != null ? role : UserRole.STUDENT, name);
                    try {
                        restClient.upsertProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, profile).execute();
                    } catch (Exception ignored) {}
                }

                if (profile.getRole() == UserRole.MERCHANT) {
                    ensureMerchantLoaded(userId, bearer, profile.getFullName(), "Student Pavilion I");
                }

                sessionManager.saveSession(accessToken, refreshToken != null ? refreshToken : "", profile);
                postSuccess(callback, profile);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Error during OAuth: " + e.getMessage(), e));
            }
        });
    }

    public void ensureMerchantLoaded(String userId, String bearer, String defaultName, String defaultLoc) {
        try {
            Response<List<Merchant>> mResp = restClient.getMerchantByOwner(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();
            if (mResp.isSuccessful() && mResp.body() != null && !mResp.body().isEmpty()) {
                Merchant m = mResp.body().get(0);
                sessionManager.saveMerchantInfo(m.getId(), m.getBusinessName(), m.getCampusLocation());
            } else {
                String bName = (defaultName != null && !defaultName.trim().isEmpty()) ? defaultName.trim() : "Merchant Outlet";
                String cLoc = (defaultLoc != null && !defaultLoc.trim().isEmpty()) ? defaultLoc.trim() : "Block C - Student Pavilion I";
                double lat = 4.337243;
                double lng = 101.142379;
                for (com.uccd3223.group13.foodhero.data.model.CampusLandmark lm : com.uccd3223.group13.foodhero.util.CampusBoundaryManager.getSeededLandmarks()) {
                    if (cLoc.contains(lm.getName())) {
                        lat = lm.getLatitude();
                        lng = lm.getLongitude();
                        break;
                    }
                }
                String mId = UUID.randomUUID().toString();
                Merchant m = new Merchant(mId, userId, bName, cLoc, lat, lng);
                Response<List<Merchant>> createResp = restClient.createMerchant(SupabaseConfig.SUPABASE_ANON_KEY, bearer, m).execute();
                if (createResp.isSuccessful() && createResp.body() != null && !createResp.body().isEmpty()) {
                    Merchant created = createResp.body().get(0);
                    sessionManager.saveMerchantInfo(created.getId(), created.getBusinessName(), created.getCampusLocation());
                } else {
                    sessionManager.saveMerchantInfo(mId, bName, cLoc);
                }
            }
        } catch (Exception e) {
            if (sessionManager.getMerchantId() == null) {
                sessionManager.saveMerchantInfo(userId, defaultName != null ? defaultName : "Merchant Outlet", defaultLoc != null ? defaultLoc : "Student Pavilion I");
            }
        }
    }

    public void restoreSession(ResultCallback<Profile> callback) {
        executor.execute(() -> {
            if (!sessionManager.isLoggedIn()) {
                postError(callback, new DataError(DataError.CODE_NOT_FOUND, "No saved session found."));
                return;
            }

            Profile cached = sessionManager.getProfile();
            String refreshToken = sessionManager.getRefreshToken();

            if (refreshToken == null || refreshToken.isEmpty()) {
                if (cached != null) {
                    postSuccess(callback, cached);
                } else {
                    postError(callback, new DataError(DataError.CODE_INVALID_CREDENTIALS, "Session expired."));
                }
                return;
            }

            try {
                // Refresh token
                AuthRequest req = AuthRequest.forRefreshToken(refreshToken);
                Response<AuthResponse> resp = authService.refreshToken(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    String newAccess = resp.body().getAccessToken();
                    String newRefresh = resp.body().getRefreshToken();
                    sessionManager.saveSession(newAccess, newRefresh, cached);
                }

                if (cached != null && cached.getRole() == UserRole.MERCHANT && sessionManager.getMerchantId() == null) {
                    String bearer = "Bearer " + sessionManager.getAccessToken();
                    ensureMerchantLoaded(cached.getId(), bearer, cached.getFullName(), "Student Pavilion I");
                }

                postSuccess(callback, sessionManager.getProfile());
            } catch (Exception e) {
                // Return cached profile if offline
                if (cached != null) {
                    postSuccess(callback, cached);
                } else {
                    postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to restore session: " + e.getMessage(), e));
                }
            }
        });
    }

    public void resetPassword(String email, ResultCallback<Void> callback) {
        executor.execute(() -> {
            try {
                AuthRequest req = new AuthRequest(email, null);
                Response<ResponseBody> resp = authService.resetPasswordForEmail(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();
                if (resp.isSuccessful()) {
                    postSuccess(callback, null);
                } else {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Unable to send password reset email."));
                }
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error: " + e.getMessage(), e));
            }
        });
    }

    public void logout(ResultCallback<Void> callback) {
        executor.execute(() -> {
            try {
                String token = sessionManager.getAccessToken();
                if (token != null) {
                    authService.logout(SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + token).execute();
                }
            } catch (Exception ignored) {
            }
            sessionManager.clearSession();
            postSuccess(callback, null);
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
