package com.uccd3223.group13.foodhero.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.CampusLandmark;
import com.uccd3223.group13.foodhero.data.model.Campus;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.model.UserRoleRecord;
import com.uccd3223.group13.foodhero.data.model.InstitutionMatch;
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
import com.google.gson.JsonObject;

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
                String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.US);
                if (role == UserRole.STUDENT) {
                    JsonObject lookup = new JsonObject();
                    lookup.addProperty("p_email", normalizedEmail);
                    Response<List<InstitutionMatch>> match = restClient.lookupInstitution(SupabaseConfig.SUPABASE_ANON_KEY, lookup).execute();
                    if (!match.isSuccessful() || match.body() == null || match.body().isEmpty()) {
                        postError(callback, new DataError(DataError.CODE_UNAUTHORIZED, "Use a supported institutional email address for Student registration."));
                        return;
                    }
                }
                java.util.Map<String, Object> metaData = new java.util.HashMap<>();
                metaData.put("requested_role", role != null ? role.name().toLowerCase() : "student");
                metaData.put("full_name", fullName);
                if (studentId != null && !studentId.isEmpty()) metaData.put("student_id", studentId);
                if (faculty != null && !faculty.isEmpty()) metaData.put("faculty", faculty);
                if (businessName != null && !businessName.isEmpty()) metaData.put("business_name", businessName);
                if (campusLocation != null && !campusLocation.isEmpty()) metaData.put("campus_location", campusLocation);

                AuthRequest req = new AuthRequest(normalizedEmail, password, metaData);
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
                String accessToken = resp.body().getAccessToken();
                String refreshToken = resp.body().getRefreshToken() != null ? resp.body().getRefreshToken() : "";
                Profile profile = new Profile(userId, email, role, fullName);
                profile.setStudentId(studentId);
                profile.setFaculty(faculty);
                if (accessToken != null && !accessToken.isEmpty() && resp.body().getUser().isEmailConfirmed()) {
                    String bearer = "Bearer " + accessToken;
                    Response<List<Profile>> verifiedProfile = restClient.getProfile(
                        SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();
                    if (verifiedProfile.isSuccessful() && verifiedProfile.body() != null && !verifiedProfile.body().isEmpty()) {
                        profile = verifiedProfile.body().get(0);
                        sessionManager.saveSession(accessToken, refreshToken, profile);
                    }
                }
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

                if (!resp.body().getUser().isEmailConfirmed()) {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED,
                        "Confirm your institutional email before signing in."));
                    return;
                }

                // Fetch Profile from database
                Response<List<Profile>> profileResp = restClient.getProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();

                Profile profile;
                if (profileResp.isSuccessful() && profileResp.body() != null && !profileResp.body().isEmpty()) {
                    profile = profileResp.body().get(0);
                } else {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED,
                        "Your trusted FoodHero profile is not ready. Please contact support."));
                    return;
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

                // The auth trigger is the only authority that may create roles/profiles.
                Response<List<Profile>> profileResp = restClient.getProfile(SupabaseConfig.SUPABASE_ANON_KEY, bearer, "eq." + userId).execute();
                Profile profile;
                if (profileResp.isSuccessful() && profileResp.body() != null && !profileResp.body().isEmpty()) {
                    profile = profileResp.body().get(0);
                } else {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED,
                        "This Google account is not eligible for a trusted FoodHero profile."));
                    return;
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
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED,
                        "This account does not have a trusted FoodHero profile."));
                    return;
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
            }
        } catch (Exception ignored) {}
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

    public void registerStudent(String email, String password, String fullName, String studentId,
            String faculty, ResultCallback<Profile> callback) {
        register(email, password, UserRole.STUDENT, fullName, studentId, faculty, null, null, callback);
    }

    public void registerMerchant(String email, String password, String ownerName,
            ResultCallback<Profile> callback) {
        register(email, password, UserRole.MERCHANT, ownerName, null, null, null, null, callback);
    }

    public void getAvailableRoles(ResultCallback<List<UserRoleRecord>> callback) {
        executor.execute(() -> {
            try {
                String userId = sessionManager.getUserId();
                if (userId == null) throw new IllegalStateException("No signed-in user");
                Response<List<UserRoleRecord>> response = restClient.getAvailableRoles(
                    SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + sessionManager.getAccessToken(), "eq." + userId).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Unable to load account roles."));
                    return;
                }
                postSuccess(callback, response.body());
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to load roles: " + e.getMessage(), e));
            }
        });
    }

    public void switchActiveRole(UserRole role, ResultCallback<Profile> callback) {
        executor.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("p_role", role.name().toLowerCase());
                Response<Profile> response = restClient.switchActiveRole(
                    SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + sessionManager.getAccessToken(), body).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED, "This role is not available for your account."));
                    return;
                }
                Profile updated = response.body();
                updated.setRole(role);
                updated.setLastActiveRole(role);
                sessionManager.updateProfile(updated);
                postSuccess(callback, updated);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to switch role: " + e.getMessage(), e));
            }
        });
    }

    public void getInstitutionsAndCampuses(ResultCallback<List<Campus>> callback) {
        executor.execute(() -> {
            try {
                Response<List<Campus>> response = restClient.getCampuses(SupabaseConfig.SUPABASE_ANON_KEY,
                    "Bearer " + sessionManager.getAccessToken(), "eq.true", "institution_code.asc,name.asc").execute();
                if (!response.isSuccessful() || response.body() == null) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Unable to load campuses."));
                    return;
                }
                postSuccess(callback, response.body());
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to load campuses: " + e.getMessage(), e));
            }
        });
    }

    public void requestInstitutionalEmailVerification(String email, ResultCallback<Void> callback) {
        executor.execute(() -> {
            try {
                String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
                if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    postError(callback, new DataError(DataError.CODE_VALIDATION_ERROR, "Enter a valid institutional email."));
                    return;
                }
                JsonObject body = new JsonObject();
                body.addProperty("email", normalized);
                Response<ResponseBody> response = restClient.requestInstitutionVerification(
                    SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + sessionManager.getAccessToken(), body).execute();
                if (!response.isSuccessful()) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR,
                        response.code() == 429 ? "Please wait before requesting another code." : "Unable to send the verification code."));
                    return;
                }
                postSuccess(callback, null);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to request verification: " + e.getMessage(), e));
            }
        });
    }

    public void confirmInstitutionalEmailVerification(String email, String code, String studentId,
            String faculty, ResultCallback<Profile> callback) {
        executor.execute(() -> {
            try {
                String normalized = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
                String normalizedCode = code == null ? "" : code.trim();
                if (!normalizedCode.matches("\\d{6}")) {
                    postError(callback, new DataError(DataError.CODE_VALIDATION_ERROR, "Enter the 6-digit verification code."));
                    return;
                }
                String normalizedStudentId = studentId == null ? "" : studentId.trim();
                String normalizedFaculty = faculty == null ? "" : faculty.trim();
                if (normalizedStudentId.length() < 3 || normalizedFaculty.length() < 2) {
                    postError(callback, new DataError(DataError.CODE_VALIDATION_ERROR,
                        "Student ID and faculty are required."));
                    return;
                }
                JsonObject body = new JsonObject();
                body.addProperty("p_email", normalized);
                body.addProperty("p_code", normalizedCode);
                body.addProperty("p_student_id", normalizedStudentId);
                body.addProperty("p_faculty", normalizedFaculty);
                Response<Profile> response = restClient.confirmInstitutionVerification(
                    SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + sessionManager.getAccessToken(), body).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    postError(callback, new DataError(DataError.CODE_UNAUTHORIZED, "The verification code is invalid or expired."));
                    return;
                }
                Profile profile = response.body();
                profile.setRole(UserRole.STUDENT);
                profile.setLastActiveRole(UserRole.STUDENT);
                sessionManager.updateProfile(profile);
                postSuccess(callback, profile);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to confirm verification: " + e.getMessage(), e));
            }
        });
    }

    public void addStudentRole(String institutionalEmail, String verificationCode, String studentId,
            String faculty, ResultCallback<Profile> callback) {
        confirmInstitutionalEmailVerification(institutionalEmail, verificationCode, studentId, faculty, callback);
    }

    public void completeMerchantRegistration(String businessName, String description, String phone,
            String duitNowName, String qrPath, Campus campus, String location, double latitude, double longitude,
            ResultCallback<Merchant> callback) {
        executor.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("p_business_name", businessName);
                body.addProperty("p_stall_description", description);
                body.addProperty("p_contact_phone", phone);
                body.addProperty("p_duitnow_display_name", duitNowName);
                body.addProperty("p_duitnow_qr_path", qrPath);
                body.addProperty("p_campus_id", campus.getId());
                body.addProperty("p_campus_location", location);
                body.addProperty("p_latitude", latitude);
                body.addProperty("p_longitude", longitude);
                Response<Merchant> response = restClient.completeMerchantRegistration(
                    SupabaseConfig.SUPABASE_ANON_KEY, "Bearer " + sessionManager.getAccessToken(), body).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    postError(callback, new DataError(DataError.CODE_SERVER_ERROR, "Merchant registration was not accepted."));
                    return;
                }
                Merchant merchant = response.body();
                sessionManager.saveMerchantInfo(merchant.getId(), merchant.getBusinessName(), merchant.getCampusLocation());
                Profile profile = sessionManager.getProfile();
                if (profile != null) {
                    profile.setRole(UserRole.MERCHANT);
                    profile.setLastActiveRole(UserRole.MERCHANT);
                    profile.setCampusId(campus.getId());
                    sessionManager.updateProfile(profile);
                }
                postSuccess(callback, merchant);
            } catch (Exception e) {
                postError(callback, new DataError(DataError.CODE_NETWORK_ERROR, "Unable to complete merchant registration: " + e.getMessage(), e));
            }
        });
    }

    public void addMerchantRole(String businessName, String description, String phone,
            String duitNowName, String qrPath, Campus campus, String location,
            double latitude, double longitude, ResultCallback<Merchant> callback) {
        completeMerchantRegistration(businessName, description, phone, duitNowName, qrPath,
            campus, location, latitude, longitude, callback);
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
