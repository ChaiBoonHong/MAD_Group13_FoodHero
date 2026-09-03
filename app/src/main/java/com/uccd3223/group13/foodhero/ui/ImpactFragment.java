package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.ImpactSummary;
import com.uccd3223.group13.foodhero.data.model.Profile;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.ui.adapter.BadgeAdapter;
import com.uccd3223.group13.foodhero.ui.adapter.LeaderboardAdapter;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.Locale;

public class ImpactFragment extends Fragment {
    private AuthRepository authRepo;
    private FoodHeroRepository foodHeroRepo;

    private TextView tvStudentName, tvStudentMeta, tvLevelBadge, tvMetricMeals, tvMetricMoney, tvMetricCo2, tvPointsBalance, tvTreeProgressText;
    private ProgressBar progressTreeGrowth;
    private RecyclerView rvBadges, rvLeaderboard;
    private MaterialButton btnLogout;

    private BadgeAdapter badgeAdapter;
    private LeaderboardAdapter leaderboardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_impact, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepo = AuthRepository.getInstance(requireContext());
        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());

        initViews(view);
        setupRecyclerViews();
        setupListeners();
        loadImpactData();
    }

    private void initViews(View view) {
        tvStudentName = view.findViewById(R.id.tv_student_name);
        tvStudentMeta = view.findViewById(R.id.tv_student_meta);
        tvLevelBadge = view.findViewById(R.id.tv_level_badge);
        tvMetricMeals = view.findViewById(R.id.tv_metric_meals);
        tvMetricMoney = view.findViewById(R.id.tv_metric_money);
        tvMetricCo2 = view.findViewById(R.id.tv_metric_co2);
        tvPointsBalance = view.findViewById(R.id.tv_points_balance);
        tvTreeProgressText = view.findViewById(R.id.tv_tree_progress_text);
        progressTreeGrowth = view.findViewById(R.id.progress_tree_growth);
        rvBadges = view.findViewById(R.id.rv_badges);
        rvLeaderboard = view.findViewById(R.id.rv_leaderboard);
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupRecyclerViews() {
        badgeAdapter = new BadgeAdapter(requireContext());
        rvBadges.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvBadges.setAdapter(badgeAdapter);

        leaderboardAdapter = new LeaderboardAdapter(requireContext());
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLeaderboard.setAdapter(leaderboardAdapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            authRepo.logout(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onError(DataError error) {
                    Intent intent = new Intent(requireContext(), AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        });
    }

    private void loadImpactData() {
        Profile profile = authRepo.getCurrentProfile();
        if (profile != null) {
            String name = profile.getFullName() != null ? profile.getFullName() : "Student Hero";
            tvStudentName.setText(name);
            String studentId = profile.getStudentId() != null ? profile.getStudentId() : "Student";
            String faculty = profile.getFaculty() != null ? profile.getFaculty() : "UTAR Kampar";
            tvStudentMeta.setText(String.format("%s • %s", studentId, faculty));
        }

        foodHeroRepo.getStudentImpact(new ResultCallback<ImpactSummary>() {
            @Override
            public void onSuccess(ImpactSummary impact) {
                if (impact == null || !isAdded()) return;

                int meals = impact.getMealsRescued();
                tvMetricMeals.setText(String.valueOf(meals));
                tvMetricMoney.setText(CurrencyUtils.format(impact.getMoneySaved()));
                tvMetricCo2.setText(String.format(Locale.US, "%.1f kg", impact.getCo2Prevented()));
                tvPointsBalance.setText(String.format(Locale.US, "%d Points", impact.getEcoPoints()));

                if (tvLevelBadge != null) {
                    if (meals >= 25) {
                        tvLevelBadge.setText("👑 Level 5: Zero-Waste Master");
                    } else if (meals >= 10) {
                        tvLevelBadge.setText("⭐ Level 4: Campus Hero");
                    } else if (meals >= 5) {
                        tvLevelBadge.setText("🛡️ Level 3: Green Guardian");
                    } else if (meals >= 1) {
                        tvLevelBadge.setText("🌿 Level 2: Food Rescuer");
                    } else {
                        tvLevelBadge.setText("🌱 Level 1: Eco Sprout");
                    }
                }

                int progress = impact.getTreeProgressPercent();
                progressTreeGrowth.setProgress(progress);
                tvTreeProgressText.setText(String.format(Locale.US, "%d/25 meals rescued to grow your next campus tree", meals));

                badgeAdapter.setItems(impact.getBadges());
                leaderboardAdapter.setItems(impact.getLeaderboard());
            }

            @Override
            public void onError(DataError error) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error loading impact data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
