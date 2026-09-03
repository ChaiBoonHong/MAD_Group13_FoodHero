package com.uccd3223.group13.foodhero.ui;

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
import com.uccd3223.group13.foodhero.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.FoodHeroNotification;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.remote.SupabaseRealtimeClient;
import com.uccd3223.group13.foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.List;
import java.util.Locale;

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

        com.uccd3223.group13.foodhero.util.SystemBarUtils.applySafeInsets(this, findViewById(R.id.root_merchant_home));

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
                showMerchantOrdersView();
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

    private void showMerchantOrdersView() {
        if (merchantNavHost == null) return;
        merchantNavHost.removeAllViews();

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setPadding(32, 32, 32, 48);

        TextView tvHeader = new TextView(this);
        tvHeader.setText("Orders & Payment Verification");
        tvHeader.setTextSize(22);
        tvHeader.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvHeader.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvSub = new TextView(this);
        tvSub.setText("Review student DuitNow receipts to confirm orders");
        tvSub.setTextSize(13);
        tvSub.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvSub.setPadding(0, 4, 0, 24);

        mainLayout.addView(tvHeader);
        mainLayout.addView(tvSub);

        String currentMerchantId = SessionManager.getInstance(this).getUserId();
        FoodHeroRepository.getInstance(this).getMerchantOrders(currentMerchantId, new ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (orders == null || orders.isEmpty()) {
                    TextView tvEmpty = new TextView(MerchantHomeActivity.this);
                    tvEmpty.setText("No orders yet");
                    tvEmpty.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                    tvEmpty.setPadding(0, 48, 0, 0);
                    mainLayout.addView(tvEmpty);
                    return;
                }

            for (Order order : orders) {
                MaterialCardView card = new MaterialCardView(MerchantHomeActivity.this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 24);
                card.setLayoutParams(lp);
                card.setRadius(24f);
                card.setCardElevation(0f);
                card.setMaxCardElevation(0f);
                card.setStrokeWidth(2);

                boolean isPending = (order.getStatus() == OrderStatus.PENDING_VERIFICATION);
                if (isPending) {
                   card.setCardBackgroundColor(getResources().getColor(R.color.colorTimerBg));
                    card .setStrokeColor(getResources().getColor(R.color.colorAccent));
                } else if (order.getStatus() == OrderStatus.RESERVED) {
                    card.setCardBackgroundColor(getResources().getColor(R.color.white));
                    card.setStrokeColor(getResources().getColor(R.color.colorPrimary));
                } else {
                    card.setCardBackgroundColor(getResources().getColor(R.color.white));
                    card.setStrokeColor(getResources().getColor(R.color.colorCardBorder));
                }

                LinearLayout cardContent = new LinearLayout(MerchantHomeActivity.this);
                cardContent.setOrientation(LinearLayout.VERTICAL);
                cardContent.setPadding(28, 28, 28, 28);

                LinearLayout row1 = new LinearLayout(MerchantHomeActivity.this);
                row1.setOrientation(LinearLayout.HORIZONTAL);
                row1.setWeightSum(1.0f);

                TextView tvCode = new TextView(MerchantHomeActivity.this);
                tvCode.setText(String.format("Order #%s", order.getOrderCode()));
                tvCode.setTextSize(16);
                tvCode.setTextColor(getResources().getColor(R.color.colorTextPrimary));
                tvCode.setTypeface(null, android.graphics.Typeface.BOLD);
                LinearLayout.LayoutParams codeLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                tvCode.setLayoutParams(codeLp);

                TextView tvStatus = new TextView(MerchantHomeActivity.this);
                if (isPending) {
                    tvStatus.setText("⏳ Pending Receipt Verification");
                    tvStatus.setTextColor(getResources().getColor(R.color.colorTimerUrgentText));
                    tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                } else if (order.getStatus() == OrderStatus.RESERVED) {
                    tvStatus.setText("✓ Confirmed / Ready");
                    tvStatus.setTextColor(getResources().getColor(R.color.colorBrandPrice));
                    tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
                } else if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                    tvStatus.setText("⏱️ Awaiting Payment (10m)");
                    tvStatus.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                } else {
                    tvStatus.setText(order.getStatus().getValue());
                    tvStatus.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                }
                tvStatus.setTextSize(12);

                row1.addView(tvCode);
                row1.addView(tvStatus);
                cardContent.addView(row1);

                String itemTitle = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Bento Bag";
                TextView tvItem = new TextView(MerchantHomeActivity.this);
                tvItem.setText(String.format(Locale.US, "%s (x%d) • %s", itemTitle, order.getQuantity(), CurrencyUtils.format(order.getFinalPaidPrice())));
                tvItem.setTextSize(14);
                tvItem.setTextColor(getResources().getColor(R.color.colorTextSecondary));
                tvItem.setPadding(0, 8, 0, 12);
                cardContent.addView(tvItem);

                if (isPending) {
                    MaterialButton btnReview = new MaterialButton(MerchantHomeActivity.this);
                    btnReview.setText("🔍 Review Payment Receipt");
                    btnReview.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
                    btnReview.setTextColor(getResources().getColor(R.color.white));
                    btnReview.setCornerRadius(18);
                    btnReview.setOnClickListener(v -> showReceiptVerificationDialog(order));
                    cardContent.addView(btnReview);
                }

                card.addView(cardContent);
                mainLayout.addView(card);
            }
        }

        @Override
        public void onError(DataError error) {
            TextView tvError = new TextView(MerchantHomeActivity.this);
            tvError.setText("Failed to load orders: " + error.getMessage());
            tvError.setTextColor(getResources().getColor(R.color.colorError));
            mainLayout.addView(tvError);
        }
    });

        scrollView.addView(mainLayout);
        merchantNavHost.addView(scrollView);
    }

    private void showReceiptVerificationDialog(Order order) {
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(36, 24, 36, 16);

        TextView tvOrderInfo = new TextView(this);
        tvOrderInfo.setText(String.format(Locale.US, "Order: #%s\nCustomer: Chai Boon Hong (Student)\nTotal Amount: %s\nPayment Method: DuitNow QR",
            order.getOrderCode(), CurrencyUtils.format(order.getFinalPaidPrice())));
        tvOrderInfo.setTextSize(14);
        tvOrderInfo.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvOrderInfo.setPadding(0, 0, 0, 16);
        dialogLayout.addView(tvOrderInfo);

        TextView tvReceiptTitle = new TextView(this);
        tvReceiptTitle.setText("Customer Uploaded Receipt:");
        tvReceiptTitle.setTextSize(12);
        tvReceiptTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvReceiptTitle.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvReceiptTitle.setPadding(0, 0, 0, 8);
        dialogLayout.addView(tvReceiptTitle);

        ImageView ivReceipt = new ImageView(this);
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 400);
        ivReceipt.setLayoutParams(imgLp);
        ivReceipt.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (order.getPaymentReceiptUrl() != null && !order.getPaymentReceiptUrl().isEmpty()) {
            try {
                ivReceipt.setImageURI(Uri.parse(order.getPaymentReceiptUrl()));
            } catch (Exception e) {
                ivReceipt.setImageResource(R.drawable.ic_foodhero_logo);
            }
        } else {
            ivReceipt.setImageResource(R.drawable.ic_foodhero_logo);
        }
        dialogLayout.addView(ivReceipt);

        new MaterialAlertDialogBuilder(this)
            .setTitle("Verify Payment Receipt")
            .setView(dialogLayout)
            .setPositiveButton("Approve & Confirm", (d, w) -> {
                FoodHeroRepository.getInstance(MerchantHomeActivity.this).verifyPaymentReceipt(order.getId(), true, new ResultCallback<Order>() {
                    @Override
                    public void onSuccess(Order o) {
                        Toast.makeText(MerchantHomeActivity.this, "✓ Order #" + order.getOrderCode() + " approved! Student notified.", Toast.LENGTH_SHORT).show();
                        showMerchantOrdersView();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(MerchantHomeActivity.this, "Failed to approve: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Reject Slip", (d, w) -> {
                FoodHeroRepository.getInstance(MerchantHomeActivity.this).verifyPaymentReceipt(order.getId(), false, new ResultCallback<Order>() {
                    @Override
                    public void onSuccess(Order o) {
                        Toast.makeText(MerchantHomeActivity.this, "Order #" + order.getOrderCode() + " receipt rejected.", Toast.LENGTH_SHORT).show();
                        showMerchantOrdersView();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(MerchantHomeActivity.this, "Failed to reject: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNeutralButton("Cancel", null)
            .show();
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
        realtimeClient = new SupabaseRealtimeClient(this);
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
