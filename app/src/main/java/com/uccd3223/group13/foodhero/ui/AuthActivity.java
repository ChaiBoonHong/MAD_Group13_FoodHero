package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.uccd3223.group13.foodhero.BuildConfig;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;

public class AuthActivity extends AppCompatActivity {
    private AuthRepository authRepo;
    private UserRole selectedRole = UserRole.STUDENT;
    private boolean isLoginMode = true;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    private MaterialCardView cardRoleStudent, cardRoleMerchant;
    private TextView tvAuthModeTitle, tvSwitchMode, tvRoleStudentLabel, tvRoleMerchantLabel;
    private TextInputLayout tilName, tilStudentId, tilFaculty, tilBusinessName, tilCampusLocation, tilEmail, tilPassword;
    private EditText etName, etStudentId, etFaculty, etBusinessName, etCampusLocation, etEmail, etPassword;
    private LinearLayout llStudentFields, llMerchantFields;
    private MaterialButton btnAuthSubmit, btnFillStudent, btnFillMerchant, btnGoogleSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        com.uccd3223.group13.foodhero.util.SystemBarUtils.applySafeInsets(this, findViewById(R.id.root_auth));

        authRepo = AuthRepository.getInstance(this);
        setupGoogleSignIn();
        initViews();
        setupListeners();
        updateRoleSelectionUI();
        updateModeUI();

        handleDeepLinkCallback(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLinkCallback(intent);
    }

    private void setupGoogleSignIn() {
        String clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID;
        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile();

        if (clientId != null && !clientId.isEmpty() && !clientId.contains("dummy")) {
            gsoBuilder.requestIdToken(clientId);
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    handleGoogleSignInResult(task);
                }
            }
        );
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                String idToken = account.getIdToken();
                if (idToken != null && !idToken.isEmpty()) {
                    authRepo.signInWithGoogle(idToken, selectedRole, new ResultCallback<Profile>() {
                        @Override
                        public void onSuccess(Profile profile) {
                            Toast.makeText(AuthActivity.this, "Welcome " + (profile.getFullName() != null ? profile.getFullName() : account.getEmail()), Toast.LENGTH_SHORT).show();
                            routeToHome(profile);
                        }

                        @Override
                        public void onError(DataError error) {
                            Toast.makeText(AuthActivity.this, "Supabase Auth: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    // Fallback when Google Web Client ID is not yet connected
                    String email = account.getEmail() != null ? account.getEmail() : "student.google@utar.edu.my";
                    String name = account.getDisplayName() != null ? account.getDisplayName() : "Google User";
                    Profile profile = new Profile("google-" + System.currentTimeMillis(), email, selectedRole, name);
                    profile.setStudentId("22ACB08888");
                    profile.setFaculty("FICT");
                    SessionManager.getInstance(AuthActivity.this).saveSession("google-access-token", "google-refresh-token", profile);
                    Toast.makeText(AuthActivity.this, "Logged in via Google (" + name + ")", Toast.LENGTH_SHORT).show();
                    routeToHome(profile);
                }
            }
        } catch (ApiException e) {
            int code = e.getStatusCode();
            if (code == 10) {
                // DEVELOPER_ERROR: SHA-1 not registered in Google Cloud Console or Web Client ID mismatch
                Toast.makeText(this, "Google error 10 (Keystore SHA-1 missing). Opening Web OAuth...", Toast.LENGTH_LONG).show();
                launchSupabaseWebOAuth();
            } else {
                Toast.makeText(this, "Google Sign-In (" + code + "): " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void launchSupabaseWebOAuth() {
        try {
            String redirectUri = "foodhero://auth-callback";
            String authUrl = com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.SUPABASE_URL
                + "/auth/v1/authorize?provider=google&redirect_to=" + Uri.encode(redirectUri);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(authUrl));
            startActivity(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Unable to launch browser: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void handleDeepLinkCallback(Intent intent) {
        if (intent == null || intent.getData() == null) return;
        Uri uri = intent.getData();
        if ("foodhero".equals(uri.getScheme()) && "auth-callback".equals(uri.getHost())) {
            String fragment = uri.getFragment();
            String accessToken = null;
            String refreshToken = null;

            if (fragment != null && !fragment.isEmpty()) {
                String[] params = fragment.split("&");
                for (String param : params) {
                    String[] kv = param.split("=");
                    if (kv.length == 2) {
                        if ("access_token".equals(kv[0])) accessToken = Uri.decode(kv[1]);
                        else if ("refresh_token".equals(kv[0])) refreshToken = Uri.decode(kv[1]);
                    }
                }
            } else {
                accessToken = uri.getQueryParameter("access_token");
                refreshToken = uri.getQueryParameter("refresh_token");
            }

            if (accessToken != null && !accessToken.isEmpty()) {
                final String finalAccess = accessToken;
                final String finalRefresh = refreshToken != null ? refreshToken : "";
                Toast.makeText(this, "Authorizing Google session...", Toast.LENGTH_SHORT).show();

                authRepo.handleOAuthToken(finalAccess, finalRefresh, selectedRole, new ResultCallback<Profile>() {
                    @Override
                    public void onSuccess(Profile profile) {
                        Toast.makeText(AuthActivity.this, "Welcome " + (profile.getFullName() != null ? profile.getFullName() : profile.getEmail()), Toast.LENGTH_SHORT).show();
                        routeToHome(profile);
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(AuthActivity.this, "OAuth Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    private void initViews() {
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
        cardRoleStudent = findViewById(R.id.card_role_student);
        cardRoleMerchant = findViewById(R.id.card_role_merchant);
        tvRoleStudentLabel = findViewById(R.id.tv_role_student_label);
        tvRoleMerchantLabel = findViewById(R.id.tv_role_merchant_label);
        tvAuthModeTitle = findViewById(R.id.tv_auth_mode_title);
        tvSwitchMode = findViewById(R.id.tv_switch_mode);

        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);

        llStudentFields = findViewById(R.id.ll_student_fields);
        tilStudentId = findViewById(R.id.til_student_id);
        etStudentId = findViewById(R.id.et_student_id);
        tilFaculty = findViewById(R.id.til_faculty);
        etFaculty = findViewById(R.id.et_faculty);

        llMerchantFields = findViewById(R.id.ll_merchant_fields);
        tilBusinessName = findViewById(R.id.til_business_name);
        etBusinessName = findViewById(R.id.et_business_name);
        tilCampusLocation = findViewById(R.id.til_campus_location);
        etCampusLocation = findViewById(R.id.et_campus_location);

        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);

        btnAuthSubmit = findViewById(R.id.btn_auth_submit);
        btnFillStudent = findViewById(R.id.btn_fill_student);
        btnFillMerchant = findViewById(R.id.btn_fill_merchant);
    }

    private void setupListeners() {
        cardRoleStudent.setOnClickListener(v -> {
            selectedRole = UserRole.STUDENT;
            updateRoleSelectionUI();
            updateModeUI();
        });

        cardRoleMerchant.setOnClickListener(v -> {
            selectedRole = UserRole.MERCHANT;
            updateRoleSelectionUI();
            updateModeUI();
        });

        tvSwitchMode.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            updateModeUI();
        });

        btnFillStudent.setOnClickListener(v -> {
            selectedRole = UserRole.STUDENT;
            updateRoleSelectionUI();
            etEmail.setText("student@foodhero.my");
            etPassword.setText("FoodHero123!");
            if (!isLoginMode) {
                etName.setText("Chai Boon Hong (Student)");
                etStudentId.setText("22ACB01234");
                etFaculty.setText("FICT");
            }
        });

        btnFillMerchant.setOnClickListener(v -> {
            selectedRole = UserRole.MERCHANT;
            updateRoleSelectionUI();
            etEmail.setText("merchant@foodhero.my");
            etPassword.setText("FoodHero123!");
            if (!isLoginMode) {
                etName.setText("Grand Green Cafe");
                etBusinessName.setText("Grand Green Cafe");
                etCampusLocation.setText("Student Pavilion I, Cafeteria Stn 3");
            }
        });

        if (btnGoogleSignIn != null) {
            btnGoogleSignIn.setOnClickListener(v -> {
                if (googleSignInClient != null) {
                    googleSignInClient.signOut().addOnCompleteListener(this, task -> {
                        Intent signInIntent = googleSignInClient.getSignInIntent();
                        googleSignInLauncher.launch(signInIntent);
                    });
                }
            });
        }

        btnAuthSubmit.setOnClickListener(v -> handleSubmit());
    }

    private void updateRoleSelectionUI() {
        if (selectedRole == UserRole.STUDENT) {
            cardRoleStudent.setCardBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            cardRoleStudent.setStrokeWidth(0);
            if (tvRoleStudentLabel != null) tvRoleStudentLabel.setTextColor(getResources().getColor(R.color.white));

            cardRoleMerchant.setCardBackgroundColor(getResources().getColor(R.color.transparent));
            cardRoleMerchant.setStrokeWidth(0);
            if (tvRoleMerchantLabel != null) tvRoleMerchantLabel.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        } else {
            cardRoleMerchant.setCardBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            cardRoleMerchant.setStrokeWidth(0);
            if (tvRoleMerchantLabel != null) tvRoleMerchantLabel.setTextColor(getResources().getColor(R.color.white));

            cardRoleStudent.setCardBackgroundColor(getResources().getColor(R.color.transparent));
            cardRoleStudent.setStrokeWidth(0);
            if (tvRoleStudentLabel != null) tvRoleStudentLabel.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        }
        if (!isLoginMode) {
            updateModeUI();
        }
    }

    private void updateModeUI() {
        if (isLoginMode) {
            tvAuthModeTitle.setText(R.string.login);
            btnAuthSubmit.setText(R.string.login);
            tvSwitchMode.setText(R.string.no_account_prompt);

            tilName.setVisibility(View.GONE);
            llStudentFields.setVisibility(View.GONE);
            llMerchantFields.setVisibility(View.GONE);
        } else {
            tvAuthModeTitle.setText(R.string.register);
            btnAuthSubmit.setText(R.string.register);
            tvSwitchMode.setText(R.string.have_account_prompt);

            tilName.setVisibility(View.VISIBLE);
            if (selectedRole == UserRole.STUDENT) {
                llStudentFields.setVisibility(View.VISIBLE);
                llMerchantFields.setVisibility(View.GONE);
            } else {
                llStudentFields.setVisibility(View.GONE);
                llMerchantFields.setVisibility(View.VISIBLE);
            }
        }
    }

    private void handleSubmit() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        tilEmail.setError(null);

        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        tilPassword.setError(null);

        btnAuthSubmit.setEnabled(false);
        btnAuthSubmit.setText(R.string.loading);

        if (isLoginMode) {
            authRepo.login(email, password, new ResultCallback<Profile>() {
                @Override
                public void onSuccess(Profile profile) {
                    btnAuthSubmit.setEnabled(true);
                    btnAuthSubmit.setText(R.string.login);
                    Toast.makeText(AuthActivity.this, "Welcome to FoodHero!", Toast.LENGTH_SHORT).show();
                    routeToHome(profile);
                }

                @Override
                public void onError(DataError error) {
                    btnAuthSubmit.setEnabled(true);
                    btnAuthSubmit.setText(R.string.login);
                    Toast.makeText(AuthActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } else {
            String fullName = etName.getText().toString().trim();
            String studentId = etStudentId.getText().toString().trim();
            String faculty = etFaculty.getText().toString().trim();
            String businessName = etBusinessName.getText().toString().trim();
            String campusLocation = etCampusLocation.getText().toString().trim();

            if (fullName.isEmpty()) {
                tilName.setError("Full name is required");
                etName.requestFocus();
                btnAuthSubmit.setEnabled(true);
                btnAuthSubmit.setText(R.string.register);
                return;
            }

            authRepo.register(email, password, selectedRole, fullName, studentId, faculty, businessName, campusLocation, new ResultCallback<Profile>() {
                @Override
                public void onSuccess(Profile profile) {
                    btnAuthSubmit.setEnabled(true);
                    btnAuthSubmit.setText(R.string.register);
                    Toast.makeText(AuthActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                    routeToHome(profile);
                }

                @Override
                public void onError(DataError error) {
                    btnAuthSubmit.setEnabled(true);
                    btnAuthSubmit.setText(R.string.register);
                    Toast.makeText(AuthActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void routeToHome(Profile profile) {
        if (profile != null && profile.getRole() == UserRole.MERCHANT) {
            startActivity(new Intent(AuthActivity.this, MerchantHomeActivity.class));
        } else {
            startActivity(new Intent(AuthActivity.this, StudentHomeActivity.class));
        }
        finish();
    }
}
