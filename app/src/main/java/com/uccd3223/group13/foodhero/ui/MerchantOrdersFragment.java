package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.model.OrderVerificationResult;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.ui.adapter.MerchantOrderAdapter;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantOrdersFragment extends Fragment implements MerchantOrderAdapter.OnMerchantOrderClickListener {

    private MaterialButton btnScanQr, btnEnterCode;
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvOrders;
    private View layoutEmpty;
    private TextView tvEmptyTitle, tvEmptyMessage;
    private MaterialButton btnEmptyAction;

    private FoodHeroRepository foodHeroRepo;
    private SessionManager sessionManager;
    private MerchantOrderAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private int selectedTabIndex = 0; // 0: All, 1: Reserved, 2: Completed, 3: Cancelled/Expired
    private final ActivityResultLauncher<Intent> pickupScanner = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                selectedTabIndex = 2;
                if (tabLayout != null && tabLayout.getTabAt(2) != null) {
                    tabLayout.getTabAt(2).select();
                }
                loadOrders();
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_merchant_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOrders();
    }

    private void initViews(View view) {
        btnScanQr = view.findViewById(R.id.btn_scan_qr_action);
        btnEnterCode = view.findViewById(R.id.btn_enter_code_action);
        tabLayout = view.findViewById(R.id.tab_layout_merchant_orders);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_merchant_orders);
        rvOrders = view.findViewById(R.id.rv_merchant_orders);
        layoutEmpty = view.findViewById(R.id.layout_empty_merchant_orders);

        if (layoutEmpty != null) {
            tvEmptyTitle = layoutEmpty.findViewById(R.id.tv_empty_title);
            tvEmptyMessage = layoutEmpty.findViewById(R.id.tv_empty_message);
            btnEmptyAction = layoutEmpty.findViewById(R.id.btn_empty_action);
            if (btnEmptyAction != null) {
                btnEmptyAction.setVisibility(View.GONE);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new MerchantOrderAdapter(requireContext(), this);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadOrders);

        btnScanQr.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MerchantQrScannerActivity.class);
            pickupScanner.launch(intent);
        });

        btnEnterCode.setOnClickListener(v -> showManualVerificationDialog(null));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTabIndex = tab.getPosition();
                filterOrders();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadOrders() {
        swipeRefresh.setRefreshing(true);
        String merchantId = sessionManager.getMerchantId() != null ? sessionManager.getMerchantId() : sessionManager.getUserId();

        foodHeroRepo.getMerchantOrders(merchantId, new ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                allOrders = orders != null ? orders : new ArrayList<>();
                filterOrders();
            }

            @Override
            public void onError(DataError error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                adapter.setItems(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                if (tvEmptyTitle != null) tvEmptyTitle.setText("Couldn’t load orders");
                if (tvEmptyMessage != null) tvEmptyMessage.setText(error.getMessage());
                if (btnEmptyAction != null) {
                    btnEmptyAction.setVisibility(View.VISIBLE);
                    btnEmptyAction.setText("Retry");
                    btnEmptyAction.setOnClickListener(v -> loadOrders());
                }
            }
        });
    }

    private void filterOrders() {
        List<Order> filtered = new ArrayList<>();

        for (Order o : allOrders) {
            if (selectedTabIndex == 0) {
                filtered.add(o);
            } else if (selectedTabIndex == 1) {
                if (o.getStatus() == OrderStatus.READY_FOR_PICKUP || o.getStatus() == OrderStatus.PENDING_VERIFICATION || o.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                    filtered.add(o);
                }
            } else if (selectedTabIndex == 2) {
                if (o.getStatus() == OrderStatus.COMPLETED) {
                    filtered.add(o);
                }
            } else {
                if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.EXPIRED || o.getStatus() == OrderStatus.PAYMENT_REJECTED || o.getStatus() == OrderStatus.NO_SHOW) {
                    filtered.add(o);
                }
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No orders in this tab");
            if (tvEmptyMessage != null) tvEmptyMessage.setText("No customer orders match the current status filter.");
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        if (order.getStatus() == OrderStatus.PENDING_VERIFICATION) {
            onReviewReceiptClick(order);
        } else if (order.getStatus() == OrderStatus.READY_FOR_PICKUP) {
            onCompletePickupClick(order);
        }
    }

    @Override
    public void onReviewReceiptClick(Order order) {
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(36, 24, 36, 16);

        TextView tvOrderInfo = new TextView(requireContext());
        String customerName = (order.getStudent() != null && order.getStudent().getFullName() != null)
            ? order.getStudent().getFullName() + " (Student)"
            : "Student Customer";
        tvOrderInfo.setText(String.format(Locale.US, "Order: #%s\nCustomer: %s\nTotal Amount: %s\nPayment Method: DuitNow QR",
            order.getOrderCode(), customerName, CurrencyUtils.format(order.getFinalPaidPrice())));
        tvOrderInfo.setTextSize(14);
        tvOrderInfo.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvOrderInfo.setPadding(0, 0, 0, 16);
        dialogLayout.addView(tvOrderInfo);

        TextView tvReceiptTitle = new TextView(requireContext());
        tvReceiptTitle.setText("Customer Uploaded DuitNow Receipt:");
        tvReceiptTitle.setTextSize(12);
        tvReceiptTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvReceiptTitle.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvReceiptTitle.setPadding(0, 0, 0, 8);
        dialogLayout.addView(tvReceiptTitle);

        ImageView ivReceipt = new ImageView(requireContext());
        LinearLayout.LayoutParams imgLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ivReceipt.setAdjustViewBounds(true);
        ivReceipt.setMaxHeight(1200);
        ivReceipt.setLayoutParams(imgLp);
        ivReceipt.setScaleType(ImageView.ScaleType.FIT_CENTER);

        if (order.getPaymentReceiptUrl() != null && !order.getPaymentReceiptUrl().isEmpty()) {
            String receiptValue = order.getPaymentReceiptUrl();
            String receiptUrl = receiptValue.startsWith("https://")
                ? receiptValue
                : com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.getPaymentReceiptUrl(receiptValue);
            GlideUrl authorizedReceipt = new GlideUrl(receiptUrl, new LazyHeaders.Builder()
                .addHeader("Authorization", "Bearer " + sessionManager.getAccessToken())
                .addHeader("apikey", com.uccd3223.group13.foodhero.data.remote.SupabaseConfig.SUPABASE_ANON_KEY)
                .build());
            Glide.with(requireContext())
                .load(authorizedReceipt)
                .placeholder(R.drawable.ic_foodhero_logo)
                .error(R.drawable.ic_foodhero_logo)
                .fitCenter()
                .into(ivReceipt);
        } else {
            ivReceipt.setImageResource(R.drawable.ic_foodhero_logo);
        }
        dialogLayout.addView(ivReceipt);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Verify Payment Receipt")
            .setView(dialogLayout)
            .setPositiveButton("Approve & Confirm", (d, w) -> {
                foodHeroRepo.verifyPaymentReceipt(order.getId(), true, null, new ResultCallback<Order>() {
                    @Override
                    public void onSuccess(Order o) {
                        Toast.makeText(requireContext(), "✓ Order #" + order.getOrderCode() + " approved! Ready for pickup.", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(requireContext(), "Failed to approve: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Reject Slip", (d, w) -> showRejectionReasonDialog(order))
            .setNeutralButton("Cancel", null)
            .show();
    }

    private void showRejectionReasonDialog(Order order) {
        final EditText reasonInput = new EditText(requireContext());
        reasonInput.setHint("Reason shown to the student");
        reasonInput.setMinLines(2);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        reasonInput.setPadding(padding, padding / 2, padding, 0);
        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reject payment receipt")
            .setMessage("A clear reason is required. Stock will be restored exactly once.")
            .setView(reasonInput)
            .setPositiveButton("Reject", null)
            .setNegativeButton("Cancel", null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = reasonInput.getText().toString().trim();
            if (reason.length() < 3) {
                reasonInput.setError("Enter a rejection reason");
                return;
            }
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            foodHeroRepo.verifyPaymentReceipt(order.getId(), false, reason, new ResultCallback<Order>() {
                    @Override
                    public void onSuccess(Order o) {
                        Toast.makeText(requireContext(), "Order #" + order.getOrderCode() + " receipt rejected.", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadOrders();
                    }

                    @Override
                    public void onError(DataError error) {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(requireContext(), "Failed to reject: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
        }));
        dialog.show();
    }

    @Override
    public void onCompletePickupClick(Order order) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Pickup Verification")
            .setMessage("Verify student pickup for Order #" + order.getOrderCode() + " (" + CurrencyUtils.format(order.getFinalPaidPrice()) + "):")
            .setPositiveButton("Verify via Code", (d, w) -> showManualVerificationDialog(order.getOrderCode()))
            .setNegativeButton("Scan QR Camera", (d, w) -> {
                Intent intent = new Intent(requireContext(), MerchantQrScannerActivity.class);
                pickupScanner.launch(intent);
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    public void showManualVerificationDialog(String prefillCode) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_manual_verify_code, null);
        EditText etCode = dialogView.findViewById(R.id.et_manual_pickup_code);

        if (prefillCode != null && !prefillCode.isEmpty()) {
            etCode.setText(prefillCode);
        }

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_manual_code_title)
            .setView(dialogView)
            .setPositiveButton("Verify & Complete", (dialog, which) -> {
                String code = etCode.getText().toString().trim();
                if (code.isEmpty()) {
                    Toast.makeText(requireContext(), "Please enter an order code or token.", Toast.LENGTH_SHORT).show();
                    return;
                }

                String merchantId = sessionManager.getMerchantId() != null ? sessionManager.getMerchantId() : sessionManager.getUserId();
                foodHeroRepo.verifyPickupToken(code, merchantId, new ResultCallback<OrderVerificationResult>() {
                    @Override
                    public void onSuccess(OrderVerificationResult result) {
                        if (result.isValid()) {
                            showVerificationSuccessDialog(result);
                        } else {
                            showVerificationErrorDialog(result.getMessage());
                        }
                        loadOrders();
                    }

                    @Override
                    public void onError(DataError error) {
                        showVerificationErrorDialog("Verification error: " + error.getMessage());
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showVerificationSuccessDialog(OrderVerificationResult result) {
        String msg = result.getMessage() != null ? result.getMessage() :
            "Pickup verified successfully! Eco-Points awarded at RM1 = 5 points.";
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show();
        selectedTabIndex = 2;
        if (tabLayout.getTabAt(2) != null) tabLayout.getTabAt(2).select();
        loadOrders();
    }

    private void showVerificationErrorDialog(String errorMsg) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Verification Failed")
            .setMessage(errorMsg)
            .setIcon(R.drawable.ic_error)
            .setPositiveButton("OK", null)
            .show();
    }
}
