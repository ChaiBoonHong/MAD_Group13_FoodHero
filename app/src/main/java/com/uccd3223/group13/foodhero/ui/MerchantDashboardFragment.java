package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.MerchantDashboardData;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.List;
import java.util.Locale;

public class MerchantDashboardFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private TextView tvMerchantName, tvMerchantLocation;
    private MaterialButton btnCreateListing, btnViewAllOrders;
    private TextView tvActiveListingsBadge, tvLowStockBadge;
    private TextView tvMetricRevenue, tvMetricFoodDiverted, tvMetricOrdersCompleted, tvMetricRating;
    private LinearLayout llRecentOrdersContainer;
    private TextView tvEmptyRecentOrders;

    private FoodHeroRepository foodHeroRepo;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_merchant_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupListeners();
        loadDashboardData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadDashboardData();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh_merchant_dashboard);
        tvMerchantName = view.findViewById(R.id.tv_merchant_name);
        tvMerchantLocation = view.findViewById(R.id.tv_merchant_location);
        btnCreateListing = view.findViewById(R.id.btn_create_listing);
        btnViewAllOrders = view.findViewById(R.id.btn_view_all_orders);
        tvActiveListingsBadge = view.findViewById(R.id.tv_active_listings_badge);
        tvLowStockBadge = view.findViewById(R.id.tv_low_stock_badge);
        tvMetricRevenue = view.findViewById(R.id.tv_metric_revenue);
        tvMetricFoodDiverted = view.findViewById(R.id.tv_metric_food_diverted);
        tvMetricOrdersCompleted = view.findViewById(R.id.tv_metric_orders_completed);
        tvMetricRating = view.findViewById(R.id.tv_metric_rating);
        llRecentOrdersContainer = view.findViewById(R.id.ll_recent_orders_container);
        tvEmptyRecentOrders = view.findViewById(R.id.tv_empty_recent_orders);

        if (sessionManager.getFullName() != null && !sessionManager.getFullName().isEmpty()) {
            tvMerchantName.setText(sessionManager.getFullName() + " 👋");
        }
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadDashboardData);

        btnCreateListing.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditListingActivity.class);
            startActivity(intent);
        });

        btnViewAllOrders.setOnClickListener(v -> {
            if (getActivity() instanceof MerchantHomeActivity) {
                ((MerchantHomeActivity) getActivity()).switchToOrdersTab();
            }
        });
    }

    private void loadDashboardData() {
        swipeRefresh.setRefreshing(true);
        String merchantId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "merchant-demo";

        foodHeroRepo.getMerchantDashboard(merchantId, new ResultCallback<MerchantDashboardData>() {
            @Override
            public void onSuccess(MerchantDashboardData data) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                bindDashboardData(data);
            }

            @Override
            public void onError(DataError error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), "Failed to load dashboard: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindDashboardData(MerchantDashboardData data) {
        if (data == null) return;

        tvMetricRevenue.setText(CurrencyUtils.format(data.getRevenueRecovered()));
        tvMetricFoodDiverted.setText(String.format(Locale.US, "%.1f kg", data.getFoodDivertedKg()));
        tvMetricOrdersCompleted.setText(String.valueOf(data.getOrdersCompleted()));
        tvMetricRating.setText(String.format(Locale.US, "%.1f", data.getAverageRating()));

        tvActiveListingsBadge.setText(String.format(Locale.US, "%d Active Bags", data.getActiveListingsCount()));
        tvLowStockBadge.setText(String.format(Locale.US, "%d Low Stock Alert", data.getLowStockAlertsCount()));

        renderRecentOrders(data.getRecentOrders());
    }

    private void renderRecentOrders(List<Order> orders) {
        llRecentOrdersContainer.removeAllViews();

        if (orders == null || orders.isEmpty()) {
            tvEmptyRecentOrders.setVisibility(View.VISIBLE);
            return;
        }

        tvEmptyRecentOrders.setVisibility(View.GONE);
        int maxToShow = Math.min(orders.size(), 3);

        for (int i = 0; i < maxToShow; i++) {
            Order order = orders.get(i);
            View cardView = createRecentOrderCard(order);
            llRecentOrdersContainer.addView(cardView);
        }
    }

    private View createRecentOrderCard(Order order) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, getResources().getDimensionPixelSize(R.dimen.spacing_8));
        card.setLayoutParams(lp);
        card.setRadius(getResources().getDimension(R.dimen.corner_card));
        card.setCardElevation(0f);
        card.setMaxCardElevation(0f);
        card.setStrokeWidth(2);
        card.setStrokeColor(getResources().getColor(R.color.colorCardBorder));
        card.setCardBackgroundColor(getResources().getColor(R.color.colorSurface));

        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(
            getResources().getDimensionPixelSize(R.dimen.spacing_12),
            getResources().getDimensionPixelSize(R.dimen.spacing_12),
            getResources().getDimensionPixelSize(R.dimen.spacing_12),
            getResources().getDimensionPixelSize(R.dimen.spacing_12)
        );

        LinearLayout row1 = new LinearLayout(requireContext());
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setWeightSum(1.0f);

        TextView tvCode = new TextView(requireContext());
        tvCode.setText(String.format("Order #%s", order.getOrderCode()));
        tvCode.setTypeface(null, android.graphics.Typeface.BOLD);
        tvCode.setTextColor(getResources().getColor(R.color.colorTextPrimary));
        tvCode.setTextSize(14);
        LinearLayout.LayoutParams codeLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        tvCode.setLayoutParams(codeLp);

        TextView tvStatus = new TextView(requireContext());
        boolean isPending = (order.getStatus() == OrderStatus.PENDING_VERIFICATION);
        if (isPending) {
            tvStatus.setText("⏳ Slip Pending");
            tvStatus.setTextColor(getResources().getColor(R.color.colorTimerUrgentText));
        } else if (order.getStatus() == OrderStatus.RESERVED) {
            tvStatus.setText("✓ Ready for Pickup");
            tvStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            tvStatus.setText("✓ Completed");
            tvStatus.setTextColor(getResources().getColor(R.color.colorSuccess));
        } else {
            tvStatus.setText(order.getStatus() != null ? order.getStatus().getValue() : "Reserved");
            tvStatus.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        }
        tvStatus.setTextSize(12);
        tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);

        row1.addView(tvCode);
        row1.addView(tvStatus);
        content.addView(row1);

        String itemTitle = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Bento Bag";
        TextView tvDetails = new TextView(requireContext());
        tvDetails.setText(String.format(Locale.US, "%s (x%d) • %s", itemTitle, order.getQuantity(), CurrencyUtils.format(order.getFinalPaidPrice())));
        tvDetails.setTextColor(getResources().getColor(R.color.colorTextSecondary));
        tvDetails.setTextSize(13);
        tvDetails.setPadding(0, 4, 0, 0);
        content.addView(tvDetails);

        card.addView(content);

        card.setOnClickListener(v -> {
            if (getActivity() instanceof MerchantHomeActivity) {
                ((MerchantHomeActivity) getActivity()).switchToOrdersTab();
            }
        });

        return card;
    }
}
