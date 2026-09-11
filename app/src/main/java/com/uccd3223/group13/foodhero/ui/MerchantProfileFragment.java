package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Merchant;
import com.uccd3223.group13.foodhero.data.model.Review;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.model.UserRoleRecord;
import com.uccd3223.group13.foodhero.data.remote.SupabaseConfig;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.ui.adapter.ReviewAdapter;
import java.util.List;

public class MerchantProfileFragment extends Fragment {

    private TextView tvBusinessName, tvLocation, tvOperatingHours, tvDescription, tvPhone;
    private TextView tvDuitNowName, tvProfileQrStatus;
    private TextView tvAvgRating, tvReviewCount, tvNoReviews;
    private ImageView ivDuitNowQr;
    private ProgressBar progressProfileQr;
    private MaterialButton btnEditProfile, btnEditDuitNowQr, btnLogout, btnSwitchToStudent;
    private RecyclerView rvReviews;

    private FoodHeroRepository foodHeroRepo;
    private AuthRepository authRepo;
    private SessionManager sessionManager;
    private ReviewAdapter reviewAdapter;
    private RoleActionState roleActionState = RoleActionState.LOADING;
    private Merchant currentMerchant;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            loadMerchantProfile();
        }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_merchant_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        authRepo = AuthRepository.getInstance(requireContext());
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMerchantProfile();
        loadReviews();
        if (authRepo != null && btnSwitchToStudent != null) loadRoleAction();
    }

    private void initViews(View view) {
        tvBusinessName = view.findViewById(R.id.tv_profile_business_name);
        tvLocation = view.findViewById(R.id.tv_profile_location);
        tvDescription = view.findViewById(R.id.tv_profile_description);
        tvPhone = view.findViewById(R.id.tv_profile_phone);
        tvOperatingHours = view.findViewById(R.id.tv_profile_operating_hours);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);

        tvDuitNowName = view.findViewById(R.id.tv_profile_duitnow_name);
        ivDuitNowQr = view.findViewById(R.id.iv_profile_duitnow_qr);
        progressProfileQr = view.findViewById(R.id.progress_profile_qr);
        tvProfileQrStatus = view.findViewById(R.id.tv_profile_qr_status);
        btnEditDuitNowQr = view.findViewById(R.id.btn_edit_duitnow_qr);

        tvAvgRating = view.findViewById(R.id.tv_profile_avg_rating);
        tvReviewCount = view.findViewById(R.id.tv_profile_review_count);
        tvNoReviews = view.findViewById(R.id.tv_no_reviews);
        btnLogout = view.findViewById(R.id.btn_merchant_logout);
        btnSwitchToStudent = view.findViewById(R.id.btn_switch_to_student);
        rvReviews = view.findViewById(R.id.rv_merchant_reviews);

        String bizName = sessionManager.getBusinessName() != null ? sessionManager.getBusinessName() : sessionManager.getFullName();
        if (bizName != null && !bizName.isEmpty()) {
            tvBusinessName.setText(bizName);
        }
        String loc = sessionManager.getCampusLocation();
        if (loc != null && !loc.isEmpty()) {
            tvLocation.setText(loc);
        }
    }

    private void loadMerchantProfile() {
        String merchantId = sessionManager.getMerchantId() != null ? sessionManager.getMerchantId() : sessionManager.getUserId();
        foodHeroRepo.getMerchantProfile(merchantId, new ResultCallback<Merchant>() {
            @Override
            public void onSuccess(Merchant merchant) {
                if (!isAdded() || merchant == null) return;
                currentMerchant = merchant;
                if (merchant.getBusinessName() != null && !merchant.getBusinessName().isEmpty()) {
                    tvBusinessName.setText(merchant.getBusinessName());
                }
                if (merchant.getCampusLocation() != null && !merchant.getCampusLocation().isEmpty()) {
                    tvLocation.setText(merchant.getCampusLocation());
                }
                if (merchant.getStallDescription() != null && !merchant.getStallDescription().isEmpty()) {
                    tvDescription.setText(merchant.getStallDescription());
                    tvDescription.setVisibility(View.VISIBLE);
                } else {
                    tvDescription.setText("No description provided yet.");
                }
                if (merchant.getContactPhone() != null && !merchant.getContactPhone().isEmpty()) {
                    tvPhone.setText("📞 " + merchant.getContactPhone());
                    tvPhone.setVisibility(View.VISIBLE);
                } else {
                    tvPhone.setText("📞 Phone not set");
                }
                if (merchant.getClosingTime() != null && !merchant.getClosingTime().isEmpty()) {
                    tvOperatingHours.setText(merchant.getClosingTime());
                }

                // Populate DuitNow Info
                if (merchant.getDuitNowDisplayName() != null && !merchant.getDuitNowDisplayName().isEmpty()) {
                    tvDuitNowName.setText(merchant.getDuitNowDisplayName());
                } else {
                    tvDuitNowName.setText("Not configured");
                }

                String qrPath = merchant.getDuitNowQrPath();
                if (qrPath != null && !qrPath.isEmpty()) {
                    String url = SupabaseConfig.getMerchantQrUrl(qrPath);
                    GlideUrl authorized = new GlideUrl(url, new LazyHeaders.Builder()
                        .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                        .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY).build());
                    Glide.with(MerchantProfileFragment.this)
                        .load(authorized)
                        .placeholder(R.drawable.ic_qr_code)
                        .error(R.drawable.ic_qr_code)
                        .into(ivDuitNowQr);
                    tvProfileQrStatus.setText("Verified Merchant QR · Tap to enlarge");
                    ivDuitNowQr.setOnClickListener(v -> showQrPreviewDialog(merchant));
                } else {
                    Glide.with(MerchantProfileFragment.this).clear(ivDuitNowQr);
                    ivDuitNowQr.setImageResource(R.drawable.ic_qr_code);
                    tvProfileQrStatus.setText("No DuitNow QR uploaded · Tap button below to add");
                    ivDuitNowQr.setOnClickListener(null);
                }
            }

            @Override
            public void onError(DataError error) {}
        });
    }

    private void showQrPreviewDialog(Merchant merchant) {
        if (!isAdded() || merchant == null || merchant.getDuitNowQrPath() == null) return;
        ImageView qrImage = new ImageView(requireContext());
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        qrImage.setPadding(padding, padding, padding, padding);
        qrImage.setAdjustViewBounds(true);
        qrImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

        String url = SupabaseConfig.getMerchantQrUrl(merchant.getDuitNowQrPath());
        GlideUrl authorized = new GlideUrl(url, new LazyHeaders.Builder()
            .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
            .addHeader("apikey", SupabaseConfig.SUPABASE_ANON_KEY).build());
        Glide.with(this).load(authorized).placeholder(R.drawable.ic_qr_code).error(R.drawable.ic_qr_code).into(qrImage);

        String payee = merchant.getDuitNowDisplayName() != null ? merchant.getDuitNowDisplayName() : merchant.getBusinessName();
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("DuitNow Payee: " + payee)
            .setMessage("Students scan this QR code to complete payments for order pickups.")
            .setView(qrImage)
            .setPositiveButton("Close", null)
            .setNegativeButton("Change QR", (dialog, which) -> openEditProfile(2))
            .show();
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(requireContext());
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> openEditProfile(1));
        btnEditDuitNowQr.setOnClickListener(v -> openEditProfile(2));
        btnSwitchToStudent.setOnClickListener(v -> handleRoleAction());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void openEditProfile(int startStep) {
        Intent intent = new Intent(requireContext(), MerchantOnboardingActivity.class);
        intent.putExtra(MerchantOnboardingActivity.EXTRA_EDIT_MODE, true);
        intent.putExtra(MerchantOnboardingActivity.EXTRA_START_STEP, startStep);
        editProfileLauncher.launch(intent);
    }

    private void handleRoleAction() {
        if (roleActionState == RoleActionState.ERROR) {
            loadRoleAction();
        } else if (roleActionState == RoleActionState.SIGNUP_REQUIRED) {
            startActivity(new Intent(requireContext(), StudentRoleSignupActivity.class));
        } else if (roleActionState == RoleActionState.SWITCH_AVAILABLE) {
            switchToStudent();
        }
    }

    private void loadRoleAction() {
        roleActionState = RoleActionState.LOADING;
        btnSwitchToStudent.setText("Checking account access…");
        btnSwitchToStudent.setEnabled(false);
        authRepo.getAvailableRoles(new ResultCallback<List<UserRoleRecord>>() {
            @Override public void onSuccess(List<UserRoleRecord> roles) {
                if (!isAdded()) return;
                boolean hasStudent = roles != null && roles.stream().anyMatch(r -> r.getRole() == UserRole.STUDENT);
                roleActionState = hasStudent ? RoleActionState.SWITCH_AVAILABLE : RoleActionState.SIGNUP_REQUIRED;
                btnSwitchToStudent.setText(hasStudent ? "Switch to Student" : "Sign up as Student");
                btnSwitchToStudent.setEnabled(true);
            }
            @Override public void onError(DataError error) {
                if (!isAdded()) return;
                roleActionState = RoleActionState.ERROR;
                btnSwitchToStudent.setText("Retry account access");
                btnSwitchToStudent.setEnabled(true);
            }
        });
    }

    private void switchToStudent() {
        btnSwitchToStudent.setEnabled(false);
        authRepo.switchActiveRole(UserRole.STUDENT, new ResultCallback<Profile>() {
            @Override public void onSuccess(Profile result) {
                if (!isAdded()) return;
                Intent intent = new Intent(requireContext(), StudentHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            @Override public void onError(DataError error) {
                if (!isAdded()) return;
                btnSwitchToStudent.setEnabled(true);
                Snackbar.make(requireView(), error.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void loadReviews() {
        String merchantId = sessionManager.getMerchantId() != null ? sessionManager.getMerchantId() : sessionManager.getUserId();

        foodHeroRepo.getMerchantReviews(merchantId, new ResultCallback<List<Review>>() {
            @Override
            public void onSuccess(List<Review> reviews) {
                if (!isAdded()) return;
                if (reviews != null && !reviews.isEmpty()) {
                    reviewAdapter.setItems(reviews);
                    tvNoReviews.setVisibility(View.GONE);
                    tvReviewCount.setText(String.format(java.util.Locale.US, "Based on %d verified student pickups", reviews.size()));
                    double sum = 0;
                    for (Review r : reviews) {
                        sum += r.getRating();
                    }
                    tvAvgRating.setText(String.format(java.util.Locale.US, "%.1f", sum / reviews.size()));
                } else {
                    reviewAdapter.setItems(new java.util.ArrayList<>());
                    tvNoReviews.setVisibility(View.VISIBLE);
                    tvReviewCount.setText("No reviews yet from student pickups");
                    tvAvgRating.setText("-");
                }
            }

            @Override
            public void onError(DataError error) {
                if (!isAdded()) return;
                reviewAdapter.setItems(new java.util.ArrayList<>());
                tvNoReviews.setVisibility(View.VISIBLE);
                tvReviewCount.setText("Reviews unavailable — pull to retry");
                tvAvgRating.setText("-");
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out of your merchant portal?")
            .setPositiveButton("Log Out", (dialog, which) -> {
                authRepo.logout(new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        navigateToAuth();
                    }

                    @Override
                    public void onError(DataError error) {
                        navigateToAuth();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void navigateToAuth() {
        Intent intent = new Intent(requireContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
