package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nmmart.retailos.databinding.ItemProductReviewBinding;
import com.nmmart.retailos.models.ProductReview;
import java.util.ArrayList;
import java.util.List;

public class ProductReviewAdapter extends RecyclerView.Adapter<ProductReviewAdapter.ViewHolder> {
    private List<ProductReview> reviews = new ArrayList<>();
    private Context context;

    public ProductReviewAdapter(Context context) {
        this.context = context;
    }

    public void setReviews(List<ProductReview> reviews) {
        if (reviews != null) {
            this.reviews = reviews;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductReviewBinding binding = ItemProductReviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductReview review = reviews.get(position);
        holder.bind(review);
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ItemProductReviewBinding binding;

        ViewHolder(ItemProductReviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ProductReview review) {
            binding.tvUserName.setText(review.getUserName());
            binding.rbRating.setRating(review.getRating());
            binding.tvReviewText.setText(review.getReviewText());
            binding.tvDate.setText(review.getCreatedAt());
        }
    }
}
