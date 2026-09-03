package com.uccd3223.group13.foodhero.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.OrderVerificationResult;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.util.SystemBarUtils;
import java.util.List;

public class MerchantQrScannerActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private DecoratedBarcodeView barcodeView;
    private MaterialButton btnToggleTorch, btnEnterManualCode, btnGrantCamera;
    private LinearLayout layoutPermissionDenied;

    private FoodHeroRepository foodHeroRepo;
    private SessionManager sessionManager;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    private boolean isTorchOn = false;
    private boolean isProcessingScan = false;

    private final BarcodeCallback scanCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null || isProcessingScan) {
                return;
            }
            isProcessingScan = true;
            barcodeView.pause();
            processPickupToken(result.getText());
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_qr_scanner);

        SystemBarUtils.applySafeInsets(this, findViewById(R.id.root_qr_scanner));

        foodHeroRepo = FoodHeroRepository.getInstance(this);
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupPermissionLauncher();
        checkCameraPermission();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_scanner);
        barcodeView = findViewById(R.id.barcode_scanner_view);
        btnToggleTorch = findViewById(R.id.btn_toggle_torch);
        btnEnterManualCode = findViewById(R.id.btn_enter_manual_code);
        btnGrantCamera = findViewById(R.id.btn_grant_camera);
        layoutPermissionDenied = findViewById(R.id.layout_permission_denied);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnToggleTorch.setOnClickListener(v -> toggleTorch());
        btnEnterManualCode.setOnClickListener(v -> showManualCodeDialog());
        btnGrantCamera.setOnClickListener(v -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA));
    }

    private void setupPermissionLauncher() {
        cameraPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startScanner();
                } else {
                    showPermissionDeniedLayout();
                }
            }
        );
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScanner() {
        layoutPermissionDenied.setVisibility(View.GONE);
        barcodeView.setVisibility(View.VISIBLE);
        barcodeView.decodeContinuous(scanCallback);
        barcodeView.resume();
    }

    private void showPermissionDeniedLayout() {
        barcodeView.setVisibility(View.GONE);
        layoutPermissionDenied.setVisibility(View.VISIBLE);
    }

    private void toggleTorch() {
        if (isTorchOn) {
            barcodeView.setTorchOff();
            isTorchOn = false;
            btnToggleTorch.setText("Torch: Off");
        } else {
            barcodeView.setTorchOn();
            isTorchOn = true;
            btnToggleTorch.setText("Torch: On");
        }
    }

    private void processPickupToken(String rawToken) {
        String merchantId = sessionManager.getMerchantId() != null ? sessionManager.getMerchantId() : sessionManager.getUserId();

        foodHeroRepo.verifyPickupToken(rawToken, merchantId, new ResultCallback<OrderVerificationResult>() {
            @Override
            public void onSuccess(OrderVerificationResult result) {
                if (result.isValid()) {
                    showSuccessDialog(result);
                } else {
                    showFailureDialog(result.getMessage());
                }
            }

            @Override
            public void onError(DataError error) {
                showFailureDialog("Verification error: " + error.getMessage());
            }
        });
    }

    private void showSuccessDialog(OrderVerificationResult result) {
        String msg = result.getMessage() != null ? result.getMessage() : "Pickup verified successfully! 10 Eco-Points awarded.";

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.pickup_verified_success)
            .setMessage(msg)
            .setIcon(R.drawable.ic_check_circle)
            .setCancelable(false)
            .setPositiveButton("Done", (d, w) -> finish())
            .setNegativeButton("Scan Another", (d, w) -> {
                isProcessingScan = false;
                barcodeView.resume();
            })
            .show();
    }

    private void showFailureDialog(String errorMsg) {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Verification Failed")
            .setMessage(errorMsg)
            .setIcon(R.drawable.ic_error)
            .setCancelable(false)
            .setPositiveButton("Retry Scan", (d, w) -> {
                isProcessingScan = false;
                barcodeView.resume();
            })
            .setNegativeButton("Enter Code", (d, w) -> showManualCodeDialog())
            .show();
    }

    private void showManualCodeDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_verify_code, null);
        EditText etCode = dialogView.findViewById(R.id.et_manual_pickup_code);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_manual_code_title)
            .setView(dialogView)
            .setPositiveButton("Verify & Complete", (dialog, which) -> {
                String code = etCode.getText().toString().trim();
                if (!code.isEmpty()) {
                    processPickupToken(code);
                } else {
                    Toast.makeText(this, "Please enter an order code.", Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                    isProcessingScan = false;
                }
            })
            .setNegativeButton("Cancel", (d, w) -> {
                isProcessingScan = false;
                barcodeView.resume();
            })
            .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            barcodeView.resume();
            isProcessingScan = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
