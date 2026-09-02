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
import com.uccd3223.group13.foodhero.data.remote.AuthRequest;
import com.uccd3223.group13.foodhero.data.remote.AuthResponse;
import com.uccd3223.group13.foodhero.data.remote.SupabaseAuthService;
import com.uccd3223.group13.foodhero.data.remote.SupabaseConfig;
import com.uccd3223.group13.foodhero.data.remote.SupabaseRestClient;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import java.util.List;
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
                AuthRequest req = new AuthRequest(email, password);
                Response<AuthResponse> resp = authService.signUp(SupabaseConfig.SUPABASE_ANON_KEY, req).execute();

                if (!resp.isSuccessful() || resp.body() == null || resp.body().getUser() == null) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Registration failed: " + resp.message()));
                    return;
                }

                String userId = resp.body().getUser().getId();
                String accessToken = resp.body().getAccessToken() != null ? resp.body().getAccessToken() : SupabaseConfig.SUPABASE_ANON_KEY;
                String refreshToken = resp.body().getRefreshToken() != null ? resp.body().getRefreshToken() : "";

                // Create Profile in database
                Profile profile = new Profile(userId, email, role, fullName);
                profile.setStudentId(studentId);
                profile.setFaculty(faculty);

                String bearer = "Bearer " + accessToken;
                Response<List<Profile>> profileResp = restClient.createProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, profile).execute();

                if (role == UserRole.MERCHANT && businessName != null && !businessName.isEmpty()) {
                    Merchant merchant = new Merchant(null, userId, businessName, campusLocation, 4.336214, 101.142111);
                    restClient.createMerchant(SupabaseConfig.SUPABASE_ANON_KEY, bearer, merchant).execute();
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
                    postError(callback, new DataError(DataError.CODE_INVALID_CREDENTIALS, "Invalid email or password."));
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
                    profile = new Profile(userId, email, UserRole.STUDENT, email.split("@")[0]);
                }

                sessionManager.saveSession(accessToken, refreshToken, profile);
                postSuccess(callback, profile);

            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Network error during login: " + e.getMessage(), e));
            }
        });
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
