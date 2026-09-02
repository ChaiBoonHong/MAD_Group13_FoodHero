package com.uccd3223.group13.mad_group13_foodhero.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Badge;
import java.util.ArrayList;
import java.util.List;

public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.ViewHolder> {
    private final Context context;
    private final List<Badge> items;

    public BadgeAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<Badge> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Badge badge = items.get(position);

        holder.tvTitle.setText(badge.getTitle());
        holder.tvDescription.setText(badge.getDescription());

        int tierColor = "gold".equals(badge.getTier()) ? R.color.badge_gold :
            "silver".equals(badge.getTier()) ? R.color.badge_silver :
            "emerald".equals(badge.getTier()) ? R.color.badge_emerald : R.color.badge_bronze;

        holder.ivIcon.setColorFilter(context.getResources().getColor(tierColor));

        if (badge.isUnlocked()) {
            holder.tvStatus.setText("Unlocked");
            holder.tvStatus.setBackgroundColor(context.getResources().getColor(R.color.colorPrimaryContainer));
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        } else {
            holder.tvStatus.setText("Locked");
            holder.tvStatus.setBackgroundColor(context.getResources().getColor(R.color.colorDivider));
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorTextSecondary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvDescription, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_badge_icon);
            tvTitle = itemView.findViewById(R.id.tv_badge_title);
            tvDescription = itemView.findViewById(R.id.tv_badge_description);
            tvStatus = itemView.findViewById(R.id.tv_badge_status);
        }
    }
}
