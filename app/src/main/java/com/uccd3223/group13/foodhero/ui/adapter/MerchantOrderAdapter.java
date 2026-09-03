package com.uccd3223.group13.foodhero.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.Order;
import com.uccd3223.group13.foodhero.data.model.OrderStatus;
import com.uccd3223.group13.foodhero.util.CurrencyUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MerchantOrderAdapter extends RecyclerView.Adapter<MerchantOrderAdapter.ViewHolder> {

    public interface OnMerchantOrderClickListener {
        void onOrderClick(Order order);
        void onReviewReceiptClick(Order order);
        void onCompletePickupClick(Order order);
    }

    private final Context context;
    private final List<Order> items;
    private final OnMerchantOrderClickListener listener;

    public MerchantOrderAdapter(Context context, OnMerchantOrderClickListener listener) {
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
        View view = LayoutInflater.from(context).inflate(R.layout.item_merchant_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = items.get(position);

        holder.tvOrderCode.setText(String.format("Order #%s", order.getOrderCode()));

        String studentName = (order.getStudent() != null && order.getStudent().getFullName() != null)
            ? order.getStudent().getFullName() + " (Student)"
            : "Student Customer";
        holder.tvCustomer.setText(studentName);

        String title = (order.getListing() != null) ? order.getListing().getTitle() : "Surplus Mystery Bag";
        holder.tvItem.setText(String.format(Locale.US, "%s (x%d)", title, order.getQuantity()));

        String pickup = String.format(Locale.US, "Pickup: %s - %s Today",
            order.getPickupStart() != null ? order.getPickupStart() : "16:30",
            order.getPickupEnd() != null ? order.getPickupEnd() : "18:00");
        holder.tvPickupTime.setText(pickup);

        holder.tvAmount.setText(CurrencyUtils.format(order.getFinalPaidPrice()));

        // Status Styling and Action Button Visibility
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.PENDING_VERIFICATION) {
            holder.tvStatus.setText("SLIP PENDING");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorWarning));
            holder.btnReviewReceipt.setVisibility(View.VISIBLE);
            holder.btnCompletePickup.setVisibility(View.GONE);
            holder.tvCompletedStamp.setVisibility(View.GONE);
        } else if (status == OrderStatus.RESERVED) {
            holder.tvStatus.setText("READY FOR PICKUP");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorPrimary));
            holder.btnReviewReceipt.setVisibility(View.GONE);
            holder.btnCompletePickup.setVisibility(View.VISIBLE);
            holder.tvCompletedStamp.setVisibility(View.GONE);
        } else if (status == OrderStatus.COMPLETED) {
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorSuccess));
            holder.btnReviewReceipt.setVisibility(View.GONE);
            holder.btnCompletePickup.setVisibility(View.GONE);
            holder.tvCompletedStamp.setVisibility(View.VISIBLE);
        } else if (status == OrderStatus.CANCELLED || status == OrderStatus.EXPIRED || status == OrderStatus.REJECTED) {
            holder.tvStatus.setText(status.getValue().toUpperCase());
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorError));
            holder.btnReviewReceipt.setVisibility(View.GONE);
            holder.btnCompletePickup.setVisibility(View.GONE);
            holder.tvCompletedStamp.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setText(status.getValue().toUpperCase());
            holder.tvStatus.getBackground().setTint(ContextCompat.getColor(context, R.color.colorTextSecondary));
            holder.btnReviewReceipt.setVisibility(View.GONE);
            holder.btnCompletePickup.setVisibility(View.GONE);
            holder.tvCompletedStamp.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onOrderClick(order);
        });

        holder.btnReviewReceipt.setOnClickListener(v -> {
            if (listener != null) listener.onReviewReceiptClick(order);
        });

        holder.btnCompletePickup.setOnClickListener(v -> {
            if (listener != null) listener.onCompletePickupClick(order);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderCode, tvStatus, tvCustomer, tvItem, tvPickupTime, tvAmount, tvCompletedStamp;
        MaterialButton btnReviewReceipt, btnCompletePickup;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderCode = itemView.findViewById(R.id.tv_merchant_order_code);
            tvStatus = itemView.findViewById(R.id.tv_merchant_order_status);
            tvCustomer = itemView.findViewById(R.id.tv_merchant_order_customer);
            tvItem = itemView.findViewById(R.id.tv_merchant_order_item);
            tvPickupTime = itemView.findViewById(R.id.tv_merchant_order_pickup_time);
            tvAmount = itemView.findViewById(R.id.tv_merchant_order_amount);
            btnReviewReceipt = itemView.findViewById(R.id.btn_review_receipt);
            btnCompletePickup = itemView.findViewById(R.id.btn_complete_pickup);
            tvCompletedStamp = itemView.findViewById(R.id.tv_order_completed_stamp);
        }
    }
}
