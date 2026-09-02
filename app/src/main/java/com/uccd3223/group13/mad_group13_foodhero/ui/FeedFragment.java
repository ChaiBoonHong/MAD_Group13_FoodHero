package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.LocalCacheRepository;
import com.uccd3223.group13.mad_group13_foodhero.ui.adapter.ListingAdapter;
import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment implements ListingAdapter.OnListingClickListener {
    private FoodHeroRepository foodHeroRepo;
    private LocalCacheRepository localCacheRepo;
    private ListingAdapter adapter;
    private List<Listing> allListings = new ArrayList<>();

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvListings;
    private EditText etSearch;
    private ImageView btnClearSearch;
    private ChipGroup chipGroupCategories;
    private View layoutEmpty, layoutError;
    private MaterialButton btnEmptyAction, btnRetry;

    private String currentSearchQuery = "";
    private int selectedChipId = R.id.chip_all;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodHeroRepo = FoodHeroRepository.getInstance(requireContext());
        localCacheRepo = LocalCacheRepository.getInstance(requireContext());

        initViews(view);
        setupRecyclerView();
        setupListeners();
        loadFeedData();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh_feed);
        rvListings = view.findViewById(R.id.rv_feed_listings);
        etSearch = view.findViewById(R.id.et_search_feed);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        chipGroupCategories = view.findViewById(R.id.chip_group_categories);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutError = view.findViewById(R.id.layout_error);

        if (layoutEmpty != null) {
            btnEmptyAction = layoutEmpty.findViewById(R.id.btn_empty_action);
        }
        if (layoutError != null) {
            btnRetry = layoutError.findViewById(R.id.btn_retry);
        }
    }

    private void setupRecyclerView() {
        adapter = new ListingAdapter(requireContext(), this);
        rvListings.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvListings.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadFeedData);

        if (btnEmptyAction != null) {
            btnEmptyAction.setOnClickListener(v -> loadFeedData());
        }
        if (btnRetry != null) {
            btnRetry.setOnClickListener(v -> loadFeedData());
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim();
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> etSearch.setText(""));

        chipGroupCategories.setOnCheckedChangeListener((group, checkedId) -> {
            selectedChipId = checkedId;
            applyFilters();
        });
    }

    private void loadFeedData() {
        swipeRefresh.setRefreshing(true);
        layoutError.setVisibility(View.GONE);

        foodHeroRepo.getActiveFeed(new ResultCallback<List<Listing>>() {
            @Override
            public void onSuccess(List<Listing> list) {
                swipeRefresh.setRefreshing(false);
                allListings = list != null ? list : new ArrayList<>();
                applyFilters();
            }

            @Override
            public void onError(DataError error) {
                swipeRefresh.setRefreshing(false);
                if (allListings.isEmpty()) {
                    layoutError.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(requireContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        List<Listing> filtered = new ArrayList<>();

        for (Listing l : allListings) {
            // Category Filter
            boolean matchesCategory = true;
            if (selectedChipId == R.id.chip_under_5) {
                matchesCategory = l.getDiscountedPrice() <= 5.00;
            } else if (selectedChipId == R.id.chip_meals) {
                matchesCategory = "Meals".equalsIgnoreCase(l.getCategory()) || "Rice & Noodles".equalsIgnoreCase(l.getCategory());
            } else if (selectedChipId == R.id.chip_bakery) {
                matchesCategory = "Bakery".equalsIgnoreCase(l.getCategory());
            } else if (selectedChipId == R.id.chip_veg) {
                matchesCategory = "Vegetarian".equalsIgnoreCase(l.getCategory());
            }

            // Search Filter
            boolean matchesSearch = true;
            if (!currentSearchQuery.isEmpty()) {
                String q = currentSearchQuery.toLowerCase();
                String title = l.getTitle() != null ? l.getTitle().toLowerCase() : "";
                String desc = l.getDescription() != null ? l.getDescription().toLowerCase() : "";
                String merchant = (l.getMerchant() != null && l.getMerchant().getBusinessName() != null)
                    ? l.getMerchant().getBusinessName().toLowerCase() : "";
                matchesSearch = title.contains(q) || desc.contains(q) || merchant.contains(q);
            }

            if (matchesCategory && matchesSearch) {
                filtered.add(l);
            }
        }

        adapter.setItems(filtered);
        layoutEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onListingClick(Listing listing) {
        Intent intent = new Intent(requireContext(), ListingDetailsActivity.class);
        intent.putExtra("extra_listing", listing);
        startActivity(intent);
    }

    @Override
    public void onReserveClick(Listing listing) {
        onListingClick(listing);
    }

    @Override
    public void onFavouriteClick(Listing listing, int position) {
        localCacheRepo.toggleFavourite(listing.getId(), new ResultCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean isFav) {
                listing.setFavourite(isFav);
                adapter.notifyItemChanged(position);
                String msg = isFav ? "Saved to Favourites" : "Removed from Favourites";
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(DataError error) {
            }
        });
    }
}
