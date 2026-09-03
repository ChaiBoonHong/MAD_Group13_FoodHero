package com.uccd3223.group13.foodhero.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.Listing;
import com.uccd3223.group13.foodhero.data.model.ListingStatus;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantListingAdapter extends RecyclerView.Adapter<MerchantListingAdapter.ViewHolder> {

    public interface OnMerchantListingClickListener {
        void onListingClick(Listing listing);
        void onEditClick(Listing listing);
        void onRestockClick(Listing listing);
        void onDeactivateClick(Listing listing);
    }

    private final Context context;
    private final List<Listing> items;
    private final OnMerchantListingClickListener listener;

    public MerchantListingAdapter(Context context, OnMerchantListingClickListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<Listing> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_merchant_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = items.get(position);

        holder.tvTitle.setText(listing.getTitle());
        holder.tvCategory.setText(listing.getCategory() != null ? listing.getCategory() : "Surplus Bag");

        String pickupText = String.format(Locale.US, "Pickup: %s - %s",
            listing.getPickupStart() != null ? listing.getPickupStart() : "16:00",
            listing.getPickupEnd() != null ? listing.getPickupEnd() : "18:00");
        holder.tvPickupTime.setText(pickupText);

        if (listing.getPickupLocation() != null && !listing.getPickupLocation().isEmpty()) {
            holder.tvLocation.setText("• " + listing.getPickupLocation());
            holder.tvLocation.setVisibility(View.VISIBLE);
        } else {
            holder.tvLocation.setVisibility(View.GONE);
        }

        int remaining = listing.getRemainingQuantity();
        int total = listing.getTotalQuantity() > 0 ? listing.getTotalQuantity() : Math.max(remaining, 1);
        holder.tvStockPill.setText(String.format(Locale.US, "%d / %d Remaining", remaining, total));

        // Determine Status Pill
        if (listing.getStatus() == ListingStatus.EXPIRED) {
            holder.tvStatus.setText("EXPIRED");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorError));
            holder.btnDeactivate.setEnabled(false);
        } else if (remaining <= 0 || listing.getStatus() == ListingStatus.SOLD_OUT) {
            holder.tvStatus.setText("SOLD OUT");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorStatusSoldOut));
            holder.btnDeactivate.setEnabled(true);
        } else if (remaining <= 2) {
            holder.tvStatus.setText("LOW STOCK");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorWarning));
            holder.btnDeactivate.setEnabled(true);
        } else {
            holder.tvStatus.setText("ACTIVE");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorSuccess));
            holder.btnDeactivate.setEnabled(true);
        }

        // Prices
        holder.tvDiscountPrice.setText(CurrencyUtils.format(listing.getDiscountedPrice()));
        holder.tvOriginalPrice.setText(CurrencyUtils.format(listing.getOriginalPrice()));
        holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        double co2 = listing.getCo2KgPerItem() > 0 ? listing.getCo2KgPerItem() : 1.2;
        holder.tvCo2.setText(String.format(Locale.US, "%.1f kg CO₂", co2));

        // Image loading via Glide
        if (listing.getImageUrl() != null && !listing.getImageUrl().trim().isEmpty()) {
            Glide.with(context)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_food_placeholder);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onListingClick(listing);
        });

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditClick(listing);
        });

        holder.btnRestock.setOnClickListener(v -> {
            if (listener != null) listener.onRestockClick(listing);
        });

        holder.btnDeactivate.setOnClickListener(v -> {
            if (listener != null) listener.onDeactivateClick(listing);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvCategory, tvStatus, tvStockPill;
        TextView tvTitle, tvPickupTime, tvLocation;
        TextView tvDiscountPrice, tvOriginalPrice, tvCo2;
        MaterialButton btnEdit, btnRestock, btnDeactivate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_merchant_listing_image);
            tvCategory = itemView.findViewById(R.id.tv_merchant_listing_category);
            tvStatus = itemView.findViewById(R.id.tv_merchant_listing_status);
            tvStockPill = itemView.findViewById(R.id.tv_merchant_listing_stock_pill);
            tvTitle = itemView.findViewById(R.id.tv_merchant_listing_title);
            tvPickupTime = itemView.findViewById(R.id.tv_merchant_listing_pickup_time);
            tvLocation = itemView.findViewById(R.id.tv_merchant_listing_location);
            tvDiscountPrice = itemView.findViewById(R.id.tv_merchant_listing_discount_price);
            tvOriginalPrice = itemView.findViewById(R.id.tv_merchant_listing_original_price);
            tvCo2 = itemView.findViewById(R.id.tv_merchant_listing_co2);
            btnEdit = itemView.findViewById(R.id.btn_edit_listing);
            btnRestock = itemView.findViewById(R.id.btn_restock_listing);
            btnDeactivate = itemView.findViewById(R.id.btn_deactivate_listing);
        }
    }
}
