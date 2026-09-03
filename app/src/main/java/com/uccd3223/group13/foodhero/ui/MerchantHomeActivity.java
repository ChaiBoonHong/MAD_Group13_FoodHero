package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.UserRole;
import com.uccd3223.group13.foodhero.data.remote.SupabaseRealtimeClient;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.util.NotificationWorker;
import com.uccd3223.group13.foodhero.util.SystemBarUtils;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MerchantHomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private FrameLayout flNotification;
    private TextView tvBadgeCount, tvAppTitle;
    private SupabaseRealtimeClient realtimeClient;
    private int unreadCount = 0;

    private final Fragment dashboardFragment = new MerchantDashboardFragment();
    private final Fragment listingsFragment = new MerchantListingsFragment();
    private final Fragment ordersFragment = new MerchantOrdersFragment();
    private final Fragment profileFragment = new MerchantProfileFragment();
    private Fragment activeFragment = dashboardFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_home);

        SystemBarUtils.applySafeInsetsWithBottomNav(
            this, findViewById(R.id.root_merchant_home), findViewById(R.id.merchant_bottom_navigation));

        initViews();
        setupNavigation();
        setupRealtimeNotifications();
        scheduleBackgroundNotificationWorker();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUnreadNotificationsCount();
    }

    private void loadUnreadNotificationsCount() {
        FoodHeroRepository.getInstance(this).getNotifications(UserRole.MERCHANT, new ResultCallback<List<FoodHeroNotification>>() {
            @Override
            public void onSuccess(List<FoodHeroNotification> list) {
                int count = 0;
                if (list != null) {
                    for (FoodHeroNotification n : list) {
                        if (!n.isRead()) count++;
                    }
                }
                unreadCount = count;
                updateBadgeCount();
            }

            @Override
            public void onError(DataError error) {
                unreadCount = 0;
                updateBadgeCount();
            }
        });
    }

    private void initViews() {
        bottomNav = findViewById(R.id.merchant_bottom_navigation);
        flNotification = findViewById(R.id.fl_notification_container);
        tvBadgeCount = findViewById(R.id.tv_notification_badge_count);
        tvAppTitle = findViewById(R.id.tv_app_title);

        if (tvAppTitle != null) {
            tvAppTitle.setText("FoodHero Merchant");
        }

        if (flNotification != null) {
            flNotification.setOnClickListener(v -> {
                Intent intent = new Intent(MerchantHomeActivity.this, NotificationsActivity.class);
                startActivity(intent);
                unreadCount = 0;
                updateBadgeCount();
            });
        }
        updateBadgeCount();
    }

    private void setupNavigation() {
        getSupportFragmentManager().beginTransaction()
            .add(R.id.merchant_nav_host_fragment, profileFragment, "4").hide(profileFragment)
            .add(R.id.merchant_nav_host_fragment, ordersFragment, "3").hide(ordersFragment)
            .add(R.id.merchant_nav_host_fragment, listingsFragment, "2").hide(listingsFragment)
            .add(R.id.merchant_nav_host_fragment, dashboardFragment, "1")
            .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(dashboardFragment).commit();
                activeFragment = dashboardFragment;
                return true;
            } else if (itemId == R.id.nav_listings) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(listingsFragment).commit();
                activeFragment = listingsFragment;
                return true;
            } else if (itemId == R.id.nav_merchant_orders) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(ordersFragment).commit();
                activeFragment = ordersFragment;
                return true;
            } else if (itemId == R.id.nav_profile) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(profileFragment).commit();
                activeFragment = profileFragment;
                return true;
            }
            return false;
        });
    }

    public void switchToOrdersTab() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_merchant_orders);
        }
    }

    private void setupRealtimeNotifications() {
        String userId = SessionManager.getInstance(this).getUserId();
        realtimeClient = new SupabaseRealtimeClient(this);
        realtimeClient.subscribe(userId, new SupabaseRealtimeClient.NotificationListener() {
            @Override
            public void onNotificationReceived(FoodHeroNotification notification) {
                runOnUiThread(() -> {
                    unreadCount++;
                    updateBadgeCount();
                });
            }
        });
    }

    private void scheduleBackgroundNotificationWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            NotificationWorker.class,
            15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FoodHeroMerchantNotificationWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        );
    }

    private void updateBadgeCount() {
        if (tvBadgeCount != null) {
            if (unreadCount > 0) {
                tvBadgeCount.setVisibility(View.VISIBLE);
                tvBadgeCount.setText(String.valueOf(unreadCount));
            } else {
                tvBadgeCount.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (realtimeClient != null) {
            realtimeClient.unsubscribe();
        }
    }
}
