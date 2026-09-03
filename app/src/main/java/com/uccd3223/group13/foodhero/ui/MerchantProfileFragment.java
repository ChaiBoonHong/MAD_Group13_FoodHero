package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Review;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.ui.adapter.ReviewAdapter;
import java.util.List;

public class MerchantProfileFragment extends Fragment {

    private TextView tvBusinessName, tvLocation, tvOperatingHours, tvAvgRating, tvReviewCount, tvNoReviews;
    private MaterialButton btnEditProfile, btnLogout;
    private RecyclerView rvReviews;

    private FoodHeroRepository foodHeroRepo;
    private AuthRepository authRepo;
    private SessionManager sessionManager;
    private ReviewAdapter reviewAdapter;

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
        loadReviews();
    }

    private void initViews(View view) {
        tvBusinessName = view.findViewById(R.id.tv_profile_business_name);
        tvLocation = view.findViewById(R.id.tv_profile_location);
        tvOperatingHours = view.findViewById(R.id.tv_profile_operating_hours);
        tvAvgRating = view.findViewById(R.id.tv_profile_avg_rating);
        tvReviewCount = view.findViewById(R.id.tv_profile_review_count);
        tvNoReviews = view.findViewById(R.id.tv_no_reviews);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnLogout = view.findViewById(R.id.btn_merchant_logout);
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

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(requireContext());
        rvReviews.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvReviews.setAdapter(reviewAdapter);
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> showEditProfileDialog());
        btnLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
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
                    for (Review r : reviews) sum += r.getRating();
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
                tvReviewCount.setText("No reviews yet from student pickups");
                tvAvgRating.setText("-");
            }
        });
    }

    private void showEditProfileDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_merchant_profile, null);
        EditText etHours = dialogView.findViewById(R.id.et_edit_hours);
        EditText etStation = dialogView.findViewById(R.id.et_edit_station);

        etHours.setText(tvOperatingHours.getText());
        etStation.setText(tvLocation.getText());

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.btn_edit_hours)
            .setView(dialogView)
            .setPositiveButton("Save", (d, w) -> {
                String newHours = etHours.getText().toString().trim();
                String newStation = etStation.getText().toString().trim();
                if (!newHours.isEmpty()) tvOperatingHours.setText(newHours);
                if (!newStation.isEmpty()) tvLocation.setText(newStation);
                Toast.makeText(requireContext(), "✓ Business information updated!", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
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
