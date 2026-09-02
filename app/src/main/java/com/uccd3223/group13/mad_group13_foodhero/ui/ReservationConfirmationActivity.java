package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.util.CurrencyUtils;
import java.util.Locale;

public class ReservationConfirmationActivity extends AppCompatActivity {
    private Order order;

    private TextView tvOrderCode, tvItemTitle, tvMerchant, tvPickupWindow, tvFinalPrice;
    private MaterialButton btnViewQr, btnBackFeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_confirmation);

        order = (Order) getIntent().getSerializableExtra("extra_order");
        if (order == null) {
            Toast.makeText(this, "Order summary unavailable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindData();
        setupListeners();
    }

    private void initViews() {
        tvOrderCode = findViewById(R.id.tv_confirm_order_code);
        tvItemTitle = findViewById(R.id.tv_confirm_item_title);
        tvMerchant = findViewById(R.id.tv_confirm_merchant);
        tvPickupWindow = findViewById(R.id.tv_confirm_pickup_window);
        tvFinalPrice = findViewById(R.id.tv_confirm_final_price);
        btnViewQr = findViewById(R.id.btn_confirm_view_qr);
        btnBackFeed = findViewById(R.id.btn_confirm_back_feed);
    }

    private void bindData() {
        tvOrderCode.setText(String.format("Order #%s", order.getOrderCode()));
        String title = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Bag";
        tvItemTitle.setText(String.format(Locale.US, "%s (x%d)", title, order.getQuantity()));

        String merchant = (order.getMerchant() != null) ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String loc = (order.getMerchant() != null) ? order.getMerchant().getCampusLocation() : "UTAR Kampar";
        tvMerchant.setText(String.format("%s • %s", merchant, loc));

        tvPickupWindow.setText(String.format("Pickup: %s - %s Today", order.getPickupStart(), order.getPickupEnd()));
        tvFinalPrice.setText(CurrencyUtils.format(order.getFinalPaidPrice()));
    }

    private void setupListeners() {
        btnViewQr.setOnClickListener(v -> {
            Intent intent = new Intent(ReservationConfirmationActivity.this, QrPickupTokenActivity.class);
            intent.putExtra("extra_order", order);
            startActivity(intent);
            finish();
        });

        btnBackFeed.setOnClickListener(v -> {
            Intent intent = new Intent(ReservationConfirmationActivity.this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
