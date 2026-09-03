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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
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
        loadOrders();
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
            startActivity(intent);
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
        String merchantId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "merchant-demo";

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
                filterOrders();
            }
        });
    }

    private void filterOrders() {
        List<Order> filtered = new ArrayList<>();

        for (Order o : allOrders) {
            if (selectedTabIndex == 0) {
                filtered.add(o);
            } else if (selectedTabIndex == 1) {
                if (o.getStatus() == OrderStatus.RESERVED || o.getStatus() == OrderStatus.PENDING_VERIFICATION || o.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                    filtered.add(o);
                }
            } else if (selectedTabIndex == 2) {
                if (o.getStatus() == OrderStatus.COMPLETED) {
                    filtered.add(o);
                }
            } else {
                if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.EXPIRED || o.getStatus() == OrderStatus.REJECTED) {
                    filtered.add(o);
                }
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (tvEmptyTitle != null) tvEmptyTitle.setText("No orders in this tab");
            if (tvEmptyMessage != null) tvEmptyMessage.setText("No customer orders match the current status filter.");
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onOrderClick(Order order) {
        if (order.getStatus() == OrderStatus.PENDING_VERIFICATION) {
            onReviewReceiptClick(order);
        } else if (order.getStatus() == OrderStatus.RESERVED) {
            onCompletePickupClick(order);
        }
    }

    @Override
    public void onReviewReceiptClick(Order order) {
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(36, 24, 36, 16);

        TextView tvOrderInfo = new TextView(requireContext());
        tvOrderInfo.setText(String.format(Locale.US, "Order: #%s\nCustomer: Chai Boon Hong (Student)\nTotal Amount: %s\nPayment Method: DuitNow QR",
            order.getOrderCode(), CurrencyUtils.format(order.getFinalPaidPrice())));
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

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Verify Payment Receipt")
            .setView(dialogLayout)
            .setPositiveButton("Approve & Confirm", (d, w) -> {
                foodHeroRepo.verifyPaymentReceipt(order.getId(), true, new ResultCallback<Order>() {
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
            .setNegativeButton("Reject Slip", (d, w) -> {
                foodHeroRepo.verifyPaymentReceipt(order.getId(), false, new ResultCallback<Order>() {
                    @Override
                    public void onSuccess(Order o) {
                        Toast.makeText(requireContext(), "Order #" + order.getOrderCode() + " receipt rejected.", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(requireContext(), "Failed to reject: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNeutralButton("Cancel", null)
            .show();
    }

    @Override
    public void onCompletePickupClick(Order order) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Complete Pickup Verification")
            .setMessage("Verify student pickup for Order #" + order.getOrderCode() + " (" + CurrencyUtils.format(order.getFinalPaidPrice()) + "):")
            .setPositiveButton("Verify via Code", (d, w) -> showManualVerificationDialog(order.getOrderCode()))
            .setNegativeButton("Scan QR Camera", (d, w) -> {
                Intent intent = new Intent(requireContext(), MerchantQrScannerActivity.class);
                startActivity(intent);
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

                String merchantId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "merchant-demo";
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
        String msg = result.getMessage() != null ? result.getMessage() : "Pickup verified successfully! 10 Eco-Points awarded.";
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.pickup_verified_success)
            .setMessage(msg)
            .setIcon(R.drawable.ic_check_circle)
            .setPositiveButton("Great!", null)
            .show();
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
