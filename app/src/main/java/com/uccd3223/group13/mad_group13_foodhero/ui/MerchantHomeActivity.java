package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.mad_group13_foodhero.data.remote.SupabaseRealtimeClient;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.mad_group13_foodhero.data.session.SessionManager;

public class MerchantHomeActivity extends AppCompatActivity {
    private BottomNavigationView bottomNav;
    private FrameLayout flNotification, merchantNavHost;
    private TextView tvBadgeCount, tvAppTitle;
    private SupabaseRealtimeClient realtimeClient;
    private int unreadCount = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_home);

        initViews();
        setupNavigation();
        setupRealtimeNotifications();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.merchant_bottom_navigation);
        flNotification = findViewById(R.id.fl_notification_container);
        tvBadgeCount = findViewById(R.id.tv_notification_badge_count);
        tvAppTitle = findViewById(R.id.tv_app_title);
        merchantNavHost = findViewById(R.id.merchant_nav_host_fragment);

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
        showMerchantPlaceholder("Dashboard", "Welcome to Merchant Portal. Ready for Fong Chee Hou's Dashboard UI.");
    }

    private void setupNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_dashboard) {
                showMerchantPlaceholder("Merchant Dashboard", "Active Listings, Revenue Recovered & Diverted Metrics");
                return true;
            } else if (itemId == R.id.nav_listings) {
                showMerchantPlaceholder("Merchant Listings", "Manage Surplus Food Bags, Dual-Source Photo Upload & Restock");
                return true;
            } else if (itemId == R.id.nav_merchant_orders) {
                showMerchantPlaceholder("Merchant Orders & QR Scanner", "Pickup Queue, Camera Scanner & Manual Token Verification");
                return true;
            } else if (itemId == R.id.nav_profile) {
                showMerchantProfilePlaceholder();
                return true;
            }
            return false;
        });
    }

    private void showMerchantPlaceholder(String title, String description) {
        if (merchantNavHost == null) return;
        merchantNavHost.removeAllViews();

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setGravity(Gravity.CENTER);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(description);
        tvDesc.setTextSize(14);
        tvDesc.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvDesc.setGravity(Gravity.CENTER);
        tvDesc.setPadding(0, 16, 0, 0);

        layout.addView(tvTitle);
        layout.addView(tvDesc);
        merchantNavHost.addView(layout);
    }

    private void showMerchantProfilePlaceholder() {
        if (merchantNavHost == null) return;
        merchantNavHost.removeAllViews();

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("Grand Green Cafe");
        tvTitle.setTextSize(20);
        tvTitle.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvLoc = new TextView(this);
        tvLoc.setText("Student Pavilion I, Cafeteria Stn 3");
        tvLoc.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvLoc.setPadding(0, 8, 0, 24);

        MaterialButton btnLogout = new MaterialButton(this);
        btnLogout.setText("Log Out");
        btnLogout.setBackgroundColor(getResources().getColor(R.color.colorError));
        btnLogout.setTextColor(getResources().getColor(R.color.white));
        btnLogout.setOnClickListener(v -> {
            AuthRepository.getInstance(MerchantHomeActivity.this).logout(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Intent intent = new Intent(MerchantHomeActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }

                @Override
                public void onError(DataError error) {
                    Intent intent = new Intent(MerchantHomeActivity.this, AuthActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        });

        layout.addView(tvTitle);
        layout.addView(tvLoc);
        layout.addView(btnLogout);
        merchantNavHost.addView(layout);
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
