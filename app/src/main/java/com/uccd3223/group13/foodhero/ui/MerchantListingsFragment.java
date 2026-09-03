package com.uccd3223.group13.foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.callback.DataError;
import com.uccd3223.group13.foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.ListingStatus;
import com.uccd3223.group13.foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.foodhero.data.session.SessionManager;
import com.uccd3223.group13.foodhero.ui.adapter.MerchantListingAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantListingsFragment extends Fragment implements MerchantListingAdapter.OnMerchantListingClickListener {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvListings;
    private ChipGroup chipGroupStatus;
    private MaterialButton btnAddListingHeader;
    private View layoutEmpty;
    private TextView tvEmptyTitle, tvEmptyMsg;
    private MaterialButton btnEmptyAction;

    private FoodHeroRepository foodHeroRepo;
    private SessionManager sessionManager;
    private MerchantListingAdapter adapter;
    private List<Listing> allListings = new ArrayList<>();
    private int selectedFilterId = R.id.chip_status_all;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_merchant_listings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        sessionManager = SessionManager.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadListings();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadListings();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh_merchant_listings);
        rvListings = view.findViewById(R.id.rv_merchant_listings);
        chipGroupStatus = view.findViewById(R.id.chip_group_status);
        btnAddListingHeader = view.findViewById(R.id.btn_add_listing_header);
        layoutEmpty = view.findViewById(R.id.layout_empty_listings);

        if (layoutEmpty != null) {
            tvEmptyTitle = layoutEmpty.findViewById(R.id.tv_empty_title);
            tvEmptyMsg = layoutEmpty.findViewById(R.id.tv_empty_message);
            btnEmptyAction = layoutEmpty.findViewById(R.id.btn_empty_action);
            if (btnEmptyAction != null) {
                btnEmptyAction.setText("+ Create First Surplus Bag");
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new MerchantListingAdapter(requireContext(), this);
        rvListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvListings.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadListings);

        btnAddListingHeader.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditListingActivity.class);
            startActivity(intent);
        });

        if (btnEmptyAction != null) {
            btnEmptyAction.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), AddEditListingActivity.class);
                startActivity(intent);
            });
        }

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                selectedFilterId = checkedIds.get(0);
                filterListings();
            }
        });
    }

    private void loadListings() {
        swipeRefresh.setRefreshing(true);
        String merchantId = sessionManager.getUserId() != null ? sessionManager.getUserId() : "merchant-demo";

        foodHeroRepo.getMerchantListings(merchantId, new ResultCallback<List<Listing>>() {
            @Override
            public void onSuccess(List<Listing> listings) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                allListings = listings != null ? listings : new ArrayList<>();
                filterListings();
            }

            @Override
            public void onError(DataError error) {
                if (!isAdded()) return;
                swipeRefresh.setRefreshing(false);
                filterListings();
            }
        });
    }

    private void filterListings() {
        List<Listing> filtered = new ArrayList<>();

        for (Listing l : allListings) {
            boolean isExpired = (l.getStatus() == ListingStatus.EXPIRED);
            boolean isSoldOut = (l.getStatus() == ListingStatus.SOLD_OUT || l.getRemainingQuantity() <= 0);
            boolean isLowStock = (l.getRemainingQuantity() > 0 && l.getRemainingQuantity() <= 2 && !isExpired);
            boolean isActive = (l.getStatus() == ListingStatus.ACTIVE && l.getRemainingQuantity() > 0);

            if (selectedFilterId == R.id.chip_status_all) {
                filtered.add(l);
            } else if (selectedFilterId == R.id.chip_status_active) {
                if (isActive) filtered.add(l);
            } else if (selectedFilterId == R.id.chip_status_low_stock) {
                if (isLowStock) filtered.add(l);
            } else if (selectedFilterId == R.id.chip_status_expired) {
                if (isExpired || isSoldOut) filtered.add(l);
            }
        }

        adapter.setItems(filtered);

        if (filtered.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            if (tvEmptyTitle != null) tvEmptyTitle.setText(R.string.empty_merchant_listings_title);
            if (tvEmptyMsg != null) tvEmptyMsg.setText(R.string.empty_merchant_listings_msg);
        } else {
            layoutEmpty.setVisibility(View.GONE);
        }
    }

    @Override
    public void onListingClick(Listing listing) {
        onEditClick(listing);
    }

    @Override
    public void onEditClick(Listing listing) {
        Intent intent = new Intent(requireContext(), AddEditListingActivity.class);
        intent.putExtra(AddEditListingActivity.EXTRA_LISTING_ID, listing.getId());
        startActivity(intent);
    }

    @Override
    public void onRestockClick(Listing listing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_restock_listing, null);
        TextView tvTitle = dialogView.findViewById(R.id.tv_restock_item_title);
        TextView tvCurrent = dialogView.findViewById(R.id.tv_restock_current_stock);
        EditText etQuantity = dialogView.findViewById(R.id.et_additional_quantity);

        tvTitle.setText(listing.getTitle());
        tvCurrent.setText(String.format(Locale.US, "Current stock: %d remaining", listing.getRemainingQuantity()));

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_restock_title)
            .setView(dialogView)
            .setPositiveButton("Restock Now", (d, which) -> {
                String qtyStr = etQuantity.getText().toString().trim();
                int qty = 5;
                if (!qtyStr.isEmpty()) {
                    try {
                        qty = Integer.parseInt(qtyStr);
                    } catch (NumberFormatException ignored) {}
                }
                final int finalQty = (qty <= 0) ? 1 : qty;

                foodHeroRepo.restockListing(listing.getId(), finalQty, new ResultCallback<Listing>() {
                    @Override
                    public void onSuccess(Listing result) {
                        Toast.makeText(requireContext(), "✓ Restocked +" + finalQty + " bags! Listing is ACTIVE.", Toast.LENGTH_SHORT).show();
                        loadListings();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(requireContext(), "Restock failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public void onDeactivateClick(Listing listing) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_deactivate_title)
            .setMessage(R.string.dialog_deactivate_msg)
            .setPositiveButton("Deactivate", (d, which) -> {
                foodHeroRepo.deactivateListing(listing.getId(), new ResultCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(requireContext(), "Listing marked as Expired.", Toast.LENGTH_SHORT).show();
                        loadListings();
                    }

                    @Override
                    public void onError(DataError error) {
                        Toast.makeText(requireContext(), "Deactivation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Keep Active", null)
            .show();
    }
}
