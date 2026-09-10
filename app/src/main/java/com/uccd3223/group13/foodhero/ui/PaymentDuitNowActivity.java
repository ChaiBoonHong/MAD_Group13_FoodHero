package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.content.ContentValues;
import android.provider.MediaStore;
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
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.Locale;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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
    private ImageView ivDuitNowQr;
    private MaterialButton btnSubmitReceipt, btnSaveQr, btnShareQr, btnCancelOrder;

    private Uri selectedReceiptUri = null;
    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_duitnow);

        com.uccd3223.group13.foodhero.util.SystemBarUtils.applySafeInsets(this, findViewById(R.id.root_payment_duitnow));

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
        ivDuitNowQr = findViewById(R.id.iv_duitnow_qr);
        btnSubmitReceipt = findViewById(R.id.btn_submit_receipt);
        btnSaveQr = findViewById(R.id.btn_save_duitnow_qr);
        btnShareQr = findViewById(R.id.btn_share_duitnow_qr);
        btnCancelOrder = findViewById(R.id.btn_cancel_unpaid_order);

        toolbar.setNavigationOnClickListener(v -> finish());

        cardReceiptUpload.setOnClickListener(v -> {
            try {
                imagePickerLauncher.launch("image/*");
            } catch (Exception e) {
                Toast.makeText(this, "Unable to open image picker", Toast.LENGTH_SHORT).show();
            }
        });

        btnSubmitReceipt.setOnClickListener(v -> submitReceipt());
        btnSaveQr.setOnClickListener(v -> saveOrShareQr(false));
        btnShareQr.setOnClickListener(v -> saveOrShareQr(true));
        btnCancelOrder.setOnClickListener(v -> confirmCancelOrder());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedReceiptUri = uri;
                    displayReceiptPreview(uri);
                }
            }
        );
    }

    private void displayReceiptPreview(Uri uri) {
        layoutUploadPlaceholder.setVisibility(View.GONE);
        layoutReceiptPreview.setVisibility(View.VISIBLE);
        ivReceiptThumbnail.setImageURI(uri);
        btnSubmitReceipt.setEnabled(true);
    }

    private void bindOrderData() {
        String merchantName = (order.getMerchant() != null && order.getMerchant().getBusinessName() != null) 
            ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String loc = (order.getMerchant() != null && order.getMerchant().getCampusLocation() != null) 
            ? order.getMerchant().getCampusLocation() : "Campus location unavailable";
        tvPayeeMerchant.setText(String.format("%s (%s)", merchantName, loc));
        tvOrderReference.setText(String.format("Reference: Order #%s", order.getOrderCode()));
        tvPaymentAmount.setText(CurrencyUtils.format(order.getFinalPaidPrice()));
        if (order.getMerchant() != null && order.getMerchant().getDuitNowQrPath() != null) {
            String url = com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.getMerchantQrUrl(order.getMerchant().getDuitNowQrPath());
            GlideUrl authorized = new GlideUrl(url, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + com.uccd3223.group13.foodhero.data.session.SessionManager.getInstance(this).getAccessToken())
                .addHeader("apikey", com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.SUPABASE_ANON_KEY).build());
            Glide.with(this).load(authorized).error(R.drawable.ic_qr_code).into(ivDuitNowQr);
        } else {
            btnSaveQr.setEnabled(false);
            btnShareQr.setEnabled(false);
            Toast.makeText(this, "Merchant DuitNow QR is unavailable. Do not make payment yet.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveOrShareQr(boolean share) {
        if (!(ivDuitNowQr.getDrawable() instanceof BitmapDrawable)) {
            Toast.makeText(this, "DuitNow QR is still loading.", Toast.LENGTH_SHORT).show();
            return;
        }
        Bitmap bitmap = ((BitmapDrawable) ivDuitNowQr.getDrawable()).getBitmap();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "FoodHero_DuitNow_" + order.getOrderCode() + ".png");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/FoodHero");
        try {
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("Could not create image");
            try (java.io.OutputStream stream = getContentResolver().openOutputStream(uri)) {
                if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IllegalStateException("Could not save image");
            }
            if (share) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("image/png");
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(intent, "Pay or share DuitNow QR"));
            } else {
                Toast.makeText(this, "DuitNow QR saved to Pictures/FoodHero.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to save QR: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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

        btnSubmitReceipt.setEnabled(false);
        btnCancelOrder.setEnabled(false);
        foodHeroRepo.expireUnpaidOrder(order.getId(), new ResultCallback<Order>() {
            @Override
            public void onSuccess(Order result) {
                showExpiredDialog();
            }

            @Override
            public void onError(DataError error) {
                btnCancelOrder.setEnabled(true);
                Toast.makeText(PaymentDuitNowActivity.this,
                    "Could not confirm expiry: " + error.getMessage() + " Retry when connected.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmCancelOrder() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Cancel unpaid order?")
            .setMessage("The reserved stock will be released after Supabase confirms the cancellation.")
            .setNegativeButton("Keep order", null)
            .setPositiveButton("Cancel order", (dialog, which) -> cancelOrder())
            .show();
    }

    private void cancelOrder() {
        btnCancelOrder.setEnabled(false);
        btnSubmitReceipt.setEnabled(false);
        foodHeroRepo.cancelUnpaidOrder(order.getId(), new ResultCallback<Order>() {
            @Override public void onSuccess(Order result) {
                if (countDownTimer != null) countDownTimer.cancel();
                Toast.makeText(PaymentDuitNowActivity.this, "Order cancelled and stock released.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(PaymentDuitNowActivity.this, StudentHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
            @Override public void onError(DataError error) {
                btnCancelOrder.setEnabled(true);
                btnSubmitReceipt.setEnabled(selectedReceiptUri != null);
                Toast.makeText(PaymentDuitNowActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
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
        if (selectedReceiptUri == null) {
            Toast.makeText(this, "Select a payment receipt image before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        btnSubmitReceipt.setEnabled(false);
        btnSubmitReceipt.setText("Uploading...");

        try (InputStream input = getContentResolver().openInputStream(selectedReceiptUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            if (bitmap == null) throw new IllegalArgumentException("Selected file is not a readable image.");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output);
            foodHeroRepo.uploadPaymentReceipt(order.getId(), output.toByteArray(), "receipt.jpg", new ResultCallback<String>() {
                @Override
                public void onSuccess(String receiptUrl) {
                    submitUploadedReceipt(receiptUrl);
                }

                @Override
                public void onError(DataError error) {
                    btnSubmitReceipt.setEnabled(true);
                    btnSubmitReceipt.setText("Submit Receipt for Verification");
                    startPaymentCountdown();
                    Toast.makeText(PaymentDuitNowActivity.this, "Upload failed: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            btnSubmitReceipt.setEnabled(true);
            btnSubmitReceipt.setText("Submit Receipt for Verification");
            startPaymentCountdown();
            Toast.makeText(this, "Unable to read receipt: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void submitUploadedReceipt(String receiptUriStr) {
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
