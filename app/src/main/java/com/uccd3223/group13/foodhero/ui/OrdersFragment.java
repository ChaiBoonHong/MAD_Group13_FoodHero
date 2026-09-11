package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.ui.adapter.OrderAdapter;
import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment implements OrderAdapter.OnOrderClickListener {
    private FoodHeroRepository foodHeroRepo;
    private OrderAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();

    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvOrders;
    private View layoutEmpty;
    private TextView tvEmptyTitle, tvEmptyMessage;
    private MaterialButton btnEmptyAction;

    private int selectedTabIndex = 0; // Payment, Verifying, Ready, Completed, Closed

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_orders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadOrders();
    }

    private void initViews(View view) {
        tabLayout = view.findViewById(R.id.tab_layout_orders);
        swipeRefresh = view.findViewById(R.id.swipe_refresh_orders);
        rvOrders = view.findViewById(R.id.rv_orders);
        layoutEmpty = view.findViewById(R.id.layout_empty);

        if (layoutEmpty != null) {
            tvEmptyTitle = layoutEmpty.findViewById(R.id.tv_empty_title);
            tvEmptyMessage = layoutEmpty.findViewById(R.id.tv_empty_message);
            btnEmptyAction = layoutEmpty.findViewById(R.id.btn_empty_action);
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(requireContext(), this);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadOrders);

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

        if (btnEmptyAction != null) {
            btnEmptyAction.setOnClickListener(v -> loadOrders());
        }
    }

    private void loadOrders() {
        swipeRefresh.setRefreshing(true);
        foodHeroRepo.getStudentOrders(new ResultCallback<List<Order>>() {
            @Override
            public void onSuccess(List<Order> orders) {
                swipeRefresh.setRefreshing(false);
                allOrders = orders != null ? orders : new ArrayList<>();
                filterOrders();
            }

            @Override
            public void onError(DataError error) {
                swipeRefresh.setRefreshing(false);
                adapter.setItems(new ArrayList<>());
                layoutEmpty.setVisibility(View.VISIBLE);
                if (tvEmptyTitle != null) tvEmptyTitle.setText("Couldn’t load orders");
                if (tvEmptyMessage != null) tvEmptyMessage.setText(error.getMessage());
                if (btnEmptyAction != null) {
                    btnEmptyAction.setVisibility(View.VISIBLE);
                    btnEmptyAction.setText("Retry");
                }
            }
        });
    }

    private void filterOrders() {
        List<Order> filtered = new ArrayList<>();

        for (Order o : allOrders) {
            OrderStatus status = o.getStatus();
            if (selectedTabIndex == 0 && status == OrderStatus.AWAITING_PAYMENT) filtered.add(o);
            else if (selectedTabIndex == 1 && status == OrderStatus.PENDING_VERIFICATION) filtered.add(o);
            else if (selectedTabIndex == 2 && status == OrderStatus.READY_FOR_PICKUP) filtered.add(o);
            else if (selectedTabIndex == 3 && status == OrderStatus.COMPLETED) filtered.add(o);
            else if (selectedTabIndex == 4 && (status == OrderStatus.CANCELLED ||
                status == OrderStatus.EXPIRED || status == OrderStatus.PAYMENT_REJECTED ||
                status == OrderStatus.NO_SHOW)) filtered.add(o);
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            String[] titles = {"No payments due", "No receipts being verified", "No pickups ready",
                "No completed pickups", "No closed orders"};
            String[] messages = {"Reserved bags awaiting payment will appear here.",
                "Orders awaiting merchant payment verification will appear here.",
                "Verified orders ready for pickup will appear here.",
                "Successfully collected orders will appear here.",
                "Cancelled, expired, rejected, and no-show orders will appear here."};
            int index = Math.min(selectedTabIndex, titles.length - 1);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(titles[index]);
            if (tvEmptyMessage != null) tvEmptyMessage.setText(messages[index]);
            if (btnEmptyAction != null) btnEmptyAction.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewQrClick(Order order) {
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            Intent intent = new Intent(requireContext(), PaymentDuitNowActivity.class);
            intent.putExtra(PaymentDuitNowActivity.EXTRA_ORDER, order);
            startActivity(intent);
        } else {
            Intent intent = new Intent(requireContext(), QrPickupTokenActivity.class);
            intent.putExtra("extra_order", order);
            startActivity(intent);
        }
    }

    @Override
    public void onRateOrderClick(Order order) {
        Intent intent = new Intent(requireContext(), ReviewActivity.class);
        intent.putExtra("extra_order", order);
        startActivity(intent);
    }
}
