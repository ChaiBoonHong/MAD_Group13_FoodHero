package com.uccd3223.group13.mad_group13_foodhero.ui.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Listing;
import com.uccd3223.group13.mad_group13_foodhero.util.CurrencyUtils;
import com.uccd3223.group13.mad_group13_foodhero.util.DateTimeUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListingAdapter extends RecyclerView.Adapter<ListingAdapter.ViewHolder> {
    private final Context context;
    private final List<Listing> items;
    private final OnListingClickListener listener;

    public interface OnListingClickListener {
        void onListingClick(Listing listing);
        void onReserveClick(Listing listing);
        void onFavouriteClick(Listing listing, int position);
    }

    public ListingAdapter(Context context, OnListingClickListener listener) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_listing_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Listing listing = items.get(position);

        holder.tvTitle.setText(listing.getTitle());
        String merchantName = listing.getMerchant() != null ? listing.getMerchant().getBusinessName() : "Campus Merchant";
        String location = listing.getPickupLocation() != null ? listing.getPickupLocation() : "UTAR Kampar";
        holder.tvMerchant.setText(String.format("%s • %s", merchantName, location));

        holder.tvCategory.setText(listing.getCategory());
        holder.tvStock.setText(String.format(Locale.US, "%d left", listing.getRemainingQuantity()));
        holder.tvCountdown.setText(DateTimeUtils.getCountdownText(listing.getPickupEnd()));

        holder.tvOriginalPrice.setText(CurrencyUtils.format(listing.getOriginalPrice()));
        holder.tvOriginalPrice.setPaintFlags(holder.tvOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.tvDiscountPrice.setText(CurrencyUtils.format(listing.getDiscountedPrice()));

        if (listing.getMerchant() != null) {
            holder.tvRating.setText(String.format(Locale.US, "%.1f", listing.getMerchant().getRating()));
        }

        // Distance & ETA text
        holder.tvDistance.setText(String.format(Locale.US, "UTAR Kampar • %s - %s", listing.getPickupStart(), listing.getPickupEnd()));

        // Favourite state
        holder.btnFavourite.setImageResource(listing.isFavourite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.btnFavourite.setColorFilter(context.getResources().getColor(listing.isFavourite() ? R.color.colorError : R.color.white));

        // Image loading (Supabase Storage or HTTPS URL with fallback)
        if (listing.getImageUrl() != null && !listing.getImageUrl().isEmpty()) {
            Glide.with(context)
                .load(listing.getImageUrl())
                .placeholder(R.drawable.ic_food_placeholder)
                .error(R.drawable.ic_food_placeholder)
                .centerCrop()
                .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_food_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onListingClick(listing);
        });

        holder.btnReserve.setOnClickListener(v -> {
            if (listener != null) listener.onReserveClick(listing);
        });

        holder.btnFavourite.setOnClickListener(v -> {
            if (listener != null) listener.onFavouriteClick(listing, position);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, btnFavourite;
        TextView tvCategory, tvCountdown, tvStock, tvTitle, tvMerchant, tvDistance, tvRating, tvOriginalPrice, tvDiscountPrice;
        MaterialButton btnReserve;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_listing_image);
            btnFavourite = itemView.findViewById(R.id.btn_favourite);
            tvCategory = itemView.findViewById(R.id.tv_category_badge);
            tvCountdown = itemView.findViewById(R.id.tv_countdown);
            tvStock = itemView.findViewById(R.id.tv_stock_badge);
            tvTitle = itemView.findViewById(R.id.tv_listing_title);
            tvMerchant = itemView.findViewById(R.id.tv_merchant_name);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvOriginalPrice = itemView.findViewById(R.id.tv_original_price);
            tvDiscountPrice = itemView.findViewById(R.id.tv_discounted_price);
            btnReserve = itemView.findViewById(R.id.btn_reserve);
        }
    }
}
