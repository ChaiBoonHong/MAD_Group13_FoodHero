package com.uccd3223.group13.foodhero.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
    private final Context context;
    private final List<Order> items;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onViewQrClick(Order order);
        void onRateOrderClick(Order order);
    }

    public OrderAdapter(Context context, OnOrderClickListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<Order> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = items.get(position);

        holder.tvOrderCode.setText(String.format("Order #%s", order.getOrderCode()));
        holder.tvStatus.setText(order.getStatus().getValue().toUpperCase(Locale.US));

        // Tint status badge
        int statusBg = (order.getStatus() == OrderStatus.RESERVED) ? R.color.colorAccent :
            (order.getStatus() == OrderStatus.COMPLETED) ? R.color.colorPrimary : R.color.colorError;
        holder.tvStatus.setBackgroundColor(context.getResources().getColor(statusBg));

        String itemTitle = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Meal Bag";
        holder.tvItemTitle.setText(String.format(Locale.US, "%s (x%d)", itemTitle, order.getQuantity()));

        String merchantName = (order.getMerchant() != null) ? order.getMerchant().getBusinessName() : "Campus Merchant";
        String location = (order.getMerchant() != null) ? order.getMerchant().getCampusLocation() : "UTAR Kampar";
        holder.tvMerchant.setText(String.format("%s • %s", merchantName, location));

        holder.tvPickupTime.setText(String.format("Pickup: %s - %s", order.getPickupStart(), order.getPickupEnd()));
        holder.tvPrice.setText(CurrencyUtils.format(order.getFinalPaidPrice()));

        if (order.getStatus() == OrderStatus.RESERVED) {
            holder.btnViewQr.setVisibility(View.VISIBLE);
            holder.btnRate.setVisibility(View.GONE);
            holder.btnViewQr.setOnClickListener(v -> {
                if (listener != null) listener.onViewQrClick(order);
            });
        } else if (order.getStatus() == OrderStatus.COMPLETED) {
            holder.btnViewQr.setVisibility(View.GONE);
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setOnClickListener(v -> {
                if (listener != null) listener.onRateOrderClick(order);
            });
        } else {
            holder.btnViewQr.setVisibility(View.GONE);
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderCode, tvStatus, tvItemTitle, tvMerchant, tvPickupTime, tvPrice;
        MaterialButton btnViewQr, btnRate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_order_code);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvItemTitle = itemView.findViewById(R.id.tv_order_item_title);
            tvMerchant = itemView.findViewById(R.id.tv_order_merchant);
            tvPickupTime = itemView.findViewById(R.id.tv_order_pickup_time);
            tvPrice = itemView.findViewById(R.id.tv_order_price);
            btnViewQr = itemView.findViewById(R.id.btn_view_qr);
            btnRate = itemView.findViewById(R.id.btn_rate_order);
        }
    }
}
