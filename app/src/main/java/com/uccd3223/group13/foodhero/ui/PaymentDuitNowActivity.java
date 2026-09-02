package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PaymentDuitNowActivity extends AppCompatActivity {
    public static final String EXTRA_ORDER = "extra_order";

    private Order order;
    private FoodHeroRepository foodHeroRepo;
    private CountDownTimer countDownTimer;

    private MaterialToolbar toolbar;
    private MaterialCardView cardTimerContainer;
    private TextView tvTimerCountdown;
    private TextView tvPayeeMerchant;
    private TextView tvOrderReference;
    private TextView tvPaymentAmount;
    private MaterialCardView cardReceiptUpload;
    private LinearLayout layoutUploadPlaceholder;
    private LinearLayout layoutReceiptPreview;
    private ImageView ivReceiptThumbnail;
    private MaterialButton btnSubmitReceipt;

    private Uri selectedReceiptUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_duitnow);

        order = (Order) getIntent().getSerializableExtra(EXTRA_ORDER);
        if (order == null) {
            Toast.makeText(this, "Order data missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        foodHeroRepo = FoodHeroRepository.getInstance(this);

        initViews();
        setupImagePicker();
        bindOrderData();
        startPaymentCountdown();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_payment);
        cardTimerContainer = findViewById(R.id.card_timer_container);
        tvTimerCountdown = findViewById(R.id.tv_timer_countdown);
        tvPayeeMerchant = findViewById(R.id.tv_payee_merchant);
        tvOrderReference = findViewById(R.id.tv_order_reference);
        tvPaymentAmount = findViewById(R.id.tv_payment_amount);
        cardReceiptUpload = findViewById(R.id.card_receipt_upload);
        layoutUploadPlaceholder = findViewById(R.id.layout_upload_placeholder);
        layoutReceiptPreview = findViewById(R.id.layout_receipt_preview);
        ivReceiptThumbnail = findViewById(R.id.iv_receipt_thumbnail);
        btnSubmitReceipt = findViewById(R.id.btn_submit_receipt);

        toolbar.setNavigationOnClickListener(v -> finish());

        cardReceiptUpload.setOnClickListener(v -> {
            try {
                imagePickerLauncher.launch("image/*");
            } catch (Exception e) {
                // Fallback demo receipt
                applyMockReceipt();
            }
        });

        btnSubmitReceipt.setOnClickListener(v -> submitReceipt());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedReceiptUri = uri;
                    displayReceiptPreview(uri);
                } else if (selectedReceiptUri == null) {
                    // Fallback demo receipt if user backs out without selecting
                    applyMockReceipt();
                }
            }
        );
    }

    private void applyMockReceipt() {
        selectedReceiptUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.ic_foodhero_logo);
        displayReceiptPreview(selectedReceiptUri);
    }

    private void displayReceiptPreview(Uri uri) {
        layoutUploadPlaceholder.setVisibility(View.GONE);
        layoutReceiptPreview.setVisibility(View.VISIBLE);
        ivReceiptThumbnail.setImageURI(uri);
        btnSubmitReceipt.setEnabled(true);
    }

    private void bindOrderData() {
        String merchantName = (order.getMerchant() != null && order.getMerchant().getBusinessName() != null) 
            ? order.getMerchant().getBusinessName() : "Grand Green Cafe";
        String loc = (order.getMerchant() != null && order.getMerchant().getCampusLocation() != null) 
            ? order.getMerchant().getCampusLocation() : "Student Pavilion I";
        tvPayeeMerchant.setText(String.format("%s (%s)", merchantName, loc));
        tvOrderReference.setText(String.format("Reference: Order #%s", order.getOrderCode()));
        tvPaymentAmount.setText(CurrencyUtils.format(order.getFinalPaidPrice()));
    }

    private void startPaymentCountdown() {
        long expiresAt = order.getPaymentExpiresAt();
        if (expiresAt <= 0) {
            expiresAt = System.currentTimeMillis() + (10 * 60 * 1000);
            order.setPaymentExpiresAt(expiresAt);
        }

        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            handleOrderExpired();
            return;
        }

        countDownTimer = new CountDownTimer(remainingMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                tvTimerCountdown.setText(String.format(Locale.US, "⏱️ Payment Window: %02d:%02d remaining", minutes, seconds));

                // Visual urgency if under 2 minutes
                if (millisUntilFinished < 120_000) {
                    cardTimerContainer.setCardBackgroundColor(ContextCompat.getColor(PaymentDuitNowActivity.this, R.color.colorTimerUrgentBg));
                    tvTimerCountdown.setTextColor(ContextCompat.getColor(PaymentDuitNowActivity.this, R.color.colorTimerUrgentText));
                }
            }

            @Override
            public void onFinish() {
                handleOrderExpired();
            }
        }.start();
    }

    private void handleOrderExpired() {
        if (isFinishing() || isDestroyed()) return;

        foodHeroRepo.cancelExpiredOrder(order.getId(), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                showExpiredDialog();
            }

            @Override
            public void onError(DataError error) {
                showExpiredDialog();
            }
        });
    }

    private void showExpiredDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Payment Window Expired")
            .setMessage("Your 10-minute payment window has expired. The reserved meal has been released back into available inventory.")
            .setCancelable(false)
            .setPositiveButton("Back to Feed", (d, w) -> {
                Intent intent = new Intent(PaymentDuitNowActivity.this, StudentHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            })
            .show();
    }

    private void submitReceipt() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        String receiptUriStr = (selectedReceiptUri != null) ? selectedReceiptUri.toString() : "sample_receipt_uri";
        btnSubmitReceipt.setEnabled(false);
        btnSubmitReceipt.setText("Submitting...");

        foodHeroRepo.submitPaymentReceipt(order.getId(), receiptUriStr, new ResultCallback<Order>() {
            @Override
            public void onSuccess(Order updatedOrder) {
                new MaterialAlertDialogBuilder(PaymentDuitNowActivity.this)
                    .setTitle("Receipt Uploaded! 🎉")
                    .setMessage(String.format("Payment slip for Order #%s has been submitted to %s.\n\nThe merchant has been notified to verify your payment. Once confirmed, your Pickup QR will be ready in the Orders tab.",
                        updatedOrder.getOrderCode(),
                        (updatedOrder.getMerchant() != null ? updatedOrder.getMerchant().getBusinessName() : "the merchant")))
                    .setCancelable(false)
                    .setPositiveButton("View in Orders", (d, w) -> {
                        Intent intent = new Intent(PaymentDuitNowActivity.this, StudentHomeActivity.class);
                        intent.putExtra("select_tab", "orders");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    })
                    .show();
            }

            @Override
            public void onError(DataError error) {
                btnSubmitReceipt.setEnabled(true);
                btnSubmitReceipt.setText("Submit Receipt for Verification");
                Toast.makeText(PaymentDuitNowActivity.this, "Submission failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
