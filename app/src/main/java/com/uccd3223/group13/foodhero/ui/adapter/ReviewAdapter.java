package com.uccd3223.group13.foodhero.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.uccd3223.group13.foodhero.R;
import com.uccd3223.group13.foodhero.data.model.Review;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private final Context context;
    private final List<Review> items;

    public ReviewAdapter(Context context) {
        this.context = context;
        this.items = new ArrayList<>();
    }

    public void setItems(List<Review> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = items.get(position);

        String reviewer = (review.getReviewer() != null) ? review.getReviewer().getFullName() : "Student Customer";
        holder.tvName.setText(reviewer);
        holder.tvRating.setText(String.format(Locale.US, "%d.0", review.getRating()));
        holder.tvComment.setText(review.getComment() != null ? review.getComment() : "Great surplus bag!");
        holder.tvDate.setText(review.getCreatedAt() != null ? review.getCreatedAt() : "Recent");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvRating, tvComment, tvDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_reviewer_name);
            tvRating = itemView.findViewById(R.id.tv_review_rating);
            tvComment = itemView.findViewById(R.id.tv_review_comment);
            tvDate = itemView.findViewById(R.id.tv_review_date);
        }
    }
}
