package com.uccd3223.group13.foodhero.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.LeaderboardEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private final Context context;
    private final List<LeaderboardEntry> items;

    public LeaderboardAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<LeaderboardEntry> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardEntry entry = items.get(position);

        holder.tvRank.setText(String.valueOf(entry.getRank()));
        holder.tvFaculty.setText(entry.getFaculty());
        holder.tvCount.setText(String.format(Locale.US, "%d meals", entry.getTotalRescued()));

        holder.tvRank.setBackgroundResource(R.drawable.bg_circle_container);
        if (entry.getRank() == 1) {
            holder.tvRank.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(context, R.color.badge_gold));
            holder.tvRank.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
        } else if (entry.getRank() == 2) {
            holder.tvRank.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(context, R.color.badge_silver));
            holder.tvRank.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
        } else if (entry.getRank() == 3) {
            holder.tvRank.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(context, R.color.badge_bronze));
            holder.tvRank.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.white));
        } else {
            holder.tvRank.setBackgroundTintList(androidx.core.content.ContextCompat.getColorStateList(context, R.color.colorPrimaryContainer));
            holder.tvRank.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.colorPrimary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank, tvFaculty, tvCount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tv_rank);
            tvFaculty = itemView.findViewById(R.id.tv_faculty_name);
            tvCount = itemView.findViewById(R.id.tv_rescued_count);
        }
    }
}
