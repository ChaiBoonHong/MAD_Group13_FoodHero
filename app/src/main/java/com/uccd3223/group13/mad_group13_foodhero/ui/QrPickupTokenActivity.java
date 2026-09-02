package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.util.QrCodeGenerator;
import java.util.Locale;

public class QrPickupTokenActivity extends AppCompatActivity {
    private Order order;

    private Toolbar toolbar;
    private ImageView ivQrCode;
    private TextView tvOrderCode, tvItemTitle, tvMerchantLocation, tvPickupWindow, tvManualCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_pickup_token);

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

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindData() {
        tvOrderCode.setText(String.format("Order #%s", order.getOrderCode()));

        String title = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Meal Bag";
        tvItemTitle.setText(String.format(Locale.US, "%s (x%d)", title, order.getQuantity()));

        String merchant = (order.getMerchant() != null) ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String loc = (order.getMerchant() != null) ? order.getMerchant().getCampusLocation() : "UTAR Kampar";
        tvMerchantLocation.setText(String.format("%s • %s", merchant, loc));

        tvPickupWindow.setText(String.format("Pickup Window: %s - %s Today", order.getPickupStart(), order.getPickupEnd()));
        tvManualCode.setText(String.format("Manual Pickup Code: %s", order.getPickupToken()));

        // Generate high-contrast QR Bitmap
        String qrPayload = order.getOrderCode() + ":" + order.getPickupToken();
        Bitmap qrBitmap = QrCodeGenerator.generateQrBitmap(qrPayload, 512, 512);
        if (qrBitmap != null) {
            ivQrCode.setImageBitmap(qrBitmap);
        }
    }
}
