package com.uccd3223.group13.foodhero.ui;

import android.graphics.Bitmap;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import com.uccd3223.group13.foodhero.util.QrCodeGenerator;
import java.util.Locale;

public class QrPickupTokenActivity extends AppCompatActivity {
    private Order order;

    private Toolbar toolbar;
    private ImageView ivQrCode;
    private TextView tvOrderCode, tvItemTitle, tvMerchantLocation, tvPickupWindow, tvManualCode;
    private TextView tvStatusTitle, tvStatusMessage;
    private MaterialButton btnRateOrder;
    private View cardPickupWarning;
    private FoodHeroRepository foodHeroRepo;
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private boolean completionShown;
    private final Runnable refreshOrder = new Runnable() {
        @Override public void run() {
            if (isFinishing() || completionShown) return;
            foodHeroRepo.getStudentOrders(new ResultCallback<List<Order>>() {
                @Override public void onSuccess(List<Order> orders) {
                    if (orders != null) {
                        for (Order current : orders) {
                            if (order.getId() != null && order.getId().equals(current.getId()) &&
                                current.getStatus() == OrderStatus.COMPLETED) {
                                order = current;
                                showCompletedState();
                                return;
                            }
                        }
                    }
                    refreshHandler.postDelayed(refreshOrder, 1500);
                }
                @Override public void onError(DataError error) {
                    refreshHandler.postDelayed(refreshOrder, 3000);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_pickup_token);
        foodHeroRepo = FoodHeroRepository.getInstance(this);

        com.uccd3223.group13.foodhero.util.SystemBarUtils.applySafeInsets(this, findViewById(R.id.root_qr_pickup_token));

        order = (Order) getIntent().getSerializableExtra("extra_order");
        if (order == null) {
            Toast.makeText(this, "Order token not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_qr);
        ivQrCode = findViewById(R.id.iv_qr_code);
        tvOrderCode = findViewById(R.id.tv_qr_order_code);
        tvItemTitle = findViewById(R.id.tv_qr_item_title);
        tvMerchantLocation = findViewById(R.id.tv_qr_merchant_location);
        tvPickupWindow = findViewById(R.id.tv_qr_pickup_window);
        tvManualCode = findViewById(R.id.tv_qr_manual_code);
        tvStatusTitle = findViewById(R.id.tv_qr_status_title);
        tvStatusMessage = findViewById(R.id.tv_qr_status_message);
        btnRateOrder = findViewById(R.id.btn_rate_completed_order);
        cardPickupWarning = findViewById(R.id.card_pickup_warning);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        btnRateOrder.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReviewActivity.class);
            intent.putExtra("extra_order", order);
            startActivity(intent);
        });
    }

    private void showCompletedState() {
        completionShown = true;
        refreshHandler.removeCallbacks(refreshOrder);
        toolbar.setTitle("Pickup Completed");
        tvStatusTitle.setText("Order Completed!");
        tvStatusMessage.setText("Your meal was collected successfully. Your Eco-Points and impact are updated.");
        ivQrCode.setImageResource(R.drawable.ic_check_circle);
        ivQrCode.setAlpha(1f);
        tvManualCode.setText("Pickup verified");
        cardPickupWarning.setVisibility(View.GONE);
        btnRateOrder.setVisibility(View.VISIBLE);
    }

    @Override protected void onResume() {
        super.onResume();
        if (!completionShown) refreshHandler.post(refreshOrder);
    }

    @Override protected void onPause() {
        refreshHandler.removeCallbacks(refreshOrder);
        super.onPause();
    }

    private void bindData() {
        tvOrderCode.setText(String.format("Order #%s", order.getOrderCode()));

        String title = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Meal Bag";
        tvItemTitle.setText(String.format(Locale.US, "%s (x%d)", title, order.getQuantity()));

        String merchant = (order.getMerchant() != null) ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String loc = (order.getMerchant() != null && order.getMerchant().getCampusLocation() != null)
            ? order.getMerchant().getCampusLocation() : "Campus location unavailable";
        tvMerchantLocation.setText(String.format("%s • %s", merchant, loc));

        tvPickupWindow.setText(String.format("Pickup Window: %s - %s Today", order.getPickupStart(), order.getPickupEnd()));

        if (order.getStatus() == com.uccd3223.group13.foodhero.data.model.OrderStatus.PENDING_VERIFICATION) {
            tvManualCode.setText("Payment verification pending...");
            ivQrCode.setImageResource(R.drawable.ic_clock); // or any placeholder
            ivQrCode.setAlpha(0.5f);
        } else if (order.getPickupToken() != null && !order.getPickupToken().isEmpty()) {
            tvManualCode.setText(String.format("Order Code: %s", order.getOrderCode()));
            // Generate high-contrast QR Bitmap
            String qrPayload = order.getOrderCode() + ":" + order.getPickupToken();
            Bitmap qrBitmap = QrCodeGenerator.generateQrBitmap(qrPayload, 512, 512);
            if (qrBitmap != null) {
                ivQrCode.setImageBitmap(qrBitmap);
                ivQrCode.setAlpha(1.0f);
            }
        } else {
            tvManualCode.setText("Token unavailable");
            ivQrCode.setAlpha(0.3f);
        }
    }
}
