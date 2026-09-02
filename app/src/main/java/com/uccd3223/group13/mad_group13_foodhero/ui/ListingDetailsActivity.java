package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Order;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Profile;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.AuthRepository;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.FoodHeroRepository;
import com.uccd3223.group13.mad_group13_foodhero.util.CurrencyUtils;
import java.util.Locale;

public class ListingDetailsActivity extends AppCompatActivity {
    private Listing listing;
    private FoodHeroRepository foodHeroRepo;
    private int selectedQuantity = 1;
    private boolean usePoints = false;
    private int availablePoints = 120;

    private ImageView ivHero;
    private Toolbar toolbar;
    private TextView tvTitle, tvMerchant, tvOriginalPrice, tvDiscountPrice, tvSavings, tvPickupWindow, tvLocation, tvDescription, tvCo2, tvQtyCount, tvPointsHint;
    private MaterialButton btnQtyMinus, btnQtyPlus, btnReserveSticky;
    private SwitchMaterial switchRedeemPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_listing_details);

        foodHeroRepo = FoodHeroRepository.getInstance(this);
        listing = (Listing) getIntent().getSerializableExtra("extra_listing");

        if (listing == null) {
            Toast.makeText(this, "Listing data not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Profile profile = AuthRepository.getInstance(this).getCurrentProfile();
        if (profile != null) {
            availablePoints = profile.getEcoPoints();
        }

        initViews();
        bindData();
        setupListeners();
        updatePriceSummary();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_details);
        ivHero = findViewById(R.id.iv_details_hero);
        tvTitle = findViewById(R.id.tv_details_title);
        tvMerchant = findViewById(R.id.tv_details_merchant);
        tvOriginalPrice = findViewById(R.id.tv_details_original_price);
        tvDiscountPrice = findViewById(R.id.tv_details_discount_price);
        tvSavings = findViewById(R.id.tv_details_savings);
        tvPickupWindow = findViewById(R.id.tv_details_pickup_window);
        tvLocation = findViewById(R.id.tv_details_location);
        tvDescription = findViewById(R.id.tv_details_description);
        tvCo2 = findViewById(R.id.tv_details_co2);
        tvQtyCount = findViewById(R.id.tv_qty_count);
        tvPointsHint = findViewById(R.id.tv_points_available_hint);

        btnQtyMinus = findViewById(R.id.btn_qty_minus);
        btnQtyPlus = findViewById(R.id.btn_qty_plus);
        btnReserveSticky = findViewById(R.id.btn_reserve_sticky);
        switchRedeemPoints = findViewById(R.id.switch_redeem_points);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void bindData() {
        tvTitle.setText(listing.getTitle());
        String merchantName = listing.getMerchant() != null ? listing.getMerchant().getBusinessName() : "Campus Merchant";
        tvMerchant.setText(String.format("%s • %s", merchantName, listing.getPickupLocation()));

        tvOriginalPrice.setText(String.format("Original: %s", CurrencyUtils.format(listing.getOriginalPrice())));
        tvOriginalPrice.setPaintFlags(tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        tvDiscountPrice.setText(CurrencyUtils.format(listing.getDiscountedPrice()));

        double savings = listing.getSavingsAmount();
        double pct = (listing.getOriginalPrice() > 0) ? (savings / listing.getOriginalPrice()) * 100.0 : 0.0;
        tvSavings.setText(String.format(Locale.US, "Save %s (%.0f%% OFF)", CurrencyUtils.format(savings), pct));

        tvPickupWindow.setText(String.format("Pickup Window: %s - %s Today", listing.getPickupStart(), listing.getPickupEnd()));
        tvLocation.setText(listing.getPickupLocation());
        tvDescription.setText(listing.getDescription());
        tvCo2.setText(String.format(Locale.US, "Rescuing this bag prevents ~%.1f kg CO₂ emissions.", listing.getCo2KgPerItem()));

        tvPointsHint.setText(String.format(Locale.US, "You have %d Eco-Points available", availablePoints));
        switchRedeemPoints.setEnabled(availablePoints >= 100);

        if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            Glide.with(this)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(ivHero);
        }
    }

    private void setupListeners() {
        btnQtyMinus.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                tvQtyCount.setText(String.valueOf(selectedQuantity));
                updatePriceSummary();
            }
        });

        btnQtyPlus.setOnClickListener(v -> {
            if (selectedQuantity < listing.getRemainingQuantity()) {
                selectedQuantity++;
                tvQtyCount.setText(String.valueOf(selectedQuantity));
                updatePriceSummary();
            } else {
                Toast.makeText(this, "Only " + listing.getRemainingQuantity() + " bags available", Toast.LENGTH_SHORT).show();
            }
        });

        switchRedeemPoints.setOnCheckedChangeListener((buttonView, isChecked) -> {
            usePoints = isChecked;
            updatePriceSummary();
        });

        btnReserveSticky.setOnClickListener(v -> handleReserve());
    }

    private void updatePriceSummary() {
        double subtotal = listing.getDiscountedPrice() * selectedQuantity;
        double discount = usePoints ? Math.min(5.00, subtotal) : 0.00;
        double total = Math.max(0.00, subtotal - discount);

        btnReserveSticky.setText(String.format(Locale.US, "Reserve for %s", CurrencyUtils.format(total)));
    }

    private void handleReserve() {
        btnReserveSticky.setEnabled(false);
        btnReserveSticky.setText("Reserving...");

        foodHeroRepo.reserveListing(listing, selectedQuantity, usePoints, new ResultCallback<Order>() {
            @Override
            public void onSuccess(Order order) {
                btnReserveSticky.setEnabled(true);
                Intent intent = new Intent(ListingDetailsActivity.this, ReservationConfirmationActivity.class);
                intent.putExtra("extra_order", order);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(DataError error) {
                btnReserveSticky.setEnabled(true);
                updatePriceSummary();
                Toast.makeText(ListingDetailsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
