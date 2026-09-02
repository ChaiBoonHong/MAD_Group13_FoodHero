package com.uccd3223.group13.mad_group13_foodhero.ui;

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
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseRealtimeClient;
import com.uccd3223.group13.mad_group13_foodhero.data.session.SessionManager;
import com.uccd3223.group13.mad_group13_foodhero.util.NotificationWorker;
import java.util.concurrent.TimeUnit;

public class StudentHomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private FrameLayout flNotification;
    private TextView tvBadgeCount;
    private SupabaseRealtimeClient realtimeClient;
    private int unreadCount = 2;

    private final Fragment feedFragment = new FeedFragment();
    private final Fragment mapFragment = new CampusMapFragment();
    private final Fragment ordersFragment = new OrdersFragment();
    private final Fragment impactFragment = new ImpactFragment();
    private Fragment activeFragment = feedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        initViews();
        setupNavigation();
        setupRealtimeNotifications();
        scheduleBackgroundNotificationWorker();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.bottom_navigation);
        flNotification = findViewById(R.id.fl_notification_container);
        tvBadgeCount = findViewById(R.id.tv_notification_badge_count);

        if (flNotification != null) {
            flNotification.setOnClickListener(v -> {
                Intent intent = new Intent(StudentHomeActivity.this, NotificationsActivity.class);
                startActivity(intent);
                unreadCount = 0;
                updateBadgeCount();
            });
        }
        updateBadgeCount();
    }

    private void setupNavigation() {
        getSupportFragmentManager().beginTransaction()
            .add(R.id.nav_host_fragment, impactFragment, "4").hide(impactFragment)
            .add(R.id.nav_host_fragment, ordersFragment, "3").hide(ordersFragment)
            .add(R.id.nav_host_fragment, mapFragment, "2").hide(mapFragment)
            .add(R.id.nav_host_fragment, feedFragment, "1")
            .commit();

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_feed) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(feedFragment).commit();
                activeFragment = feedFragment;
                return true;
            } else if (itemId == R.id.nav_map) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(mapFragment).commit();
                activeFragment = mapFragment;
                return true;
            } else if (itemId == R.id.nav_orders) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(ordersFragment).commit();
                activeFragment = ordersFragment;
                return true;
            } else if (itemId == R.id.nav_impact) {
                getSupportFragmentManager().beginTransaction().hide(activeFragment).show(impactFragment).commit();
                activeFragment = impactFragment;
                return true;
            }
            return false;
        });
    }

    private void setupRealtimeNotifications() {
        String userId = SessionManager.getInstance(this).getUserId();
        realtimeClient = new SupabaseRealtimeClient();
        realtimeClient.subscribe(userId, new SupabaseRealtimeClient.NotificationListener() {
            @Override
            public void onNotificationReceived(FoodHeroNotification notification) {
                unreadCount++;
                updateBadgeCount();
            }
        });
    }

    private void scheduleBackgroundNotificationWorker() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
            NotificationWorker.class,
            15, TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "FoodHeroNotificationWork",
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
