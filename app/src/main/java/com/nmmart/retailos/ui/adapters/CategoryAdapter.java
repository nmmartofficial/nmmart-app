package com.nmmart.retailos.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.utils.NMMartLogger;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    // Data & Listener
    private List<Category> categories = new ArrayList<>();
    private OnCategoryClickListener listener;

    // Click listener interface
    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    // Constructor
    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        this.categories = categories != null ? categories : new ArrayList<>();
        this.listener = listener;
    }

    // Update categories data
    public void setCategories(List<Category> categories) {
        this.categories = categories != null ? categories : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        try {
            if (position >= 0 && position < categories.size()) {
                holder.bind(categories.get(position));
            }
        } catch (Exception e) {
            NMMartLogger.logError("CategoryAdapter.java", "onBindViewHolder", e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    // ViewHolder class
    class CategoryViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategoryIcon;
        TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);
            
            // Handle clicks in ViewHolder (best practice)
            itemView.setOnClickListener(v -> {
                try {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION && listener != null && position < categories.size()) {
                        NMMartLogger.logClick("CategoryItem");
                        listener.onCategoryClick(categories.get(position));
                    }
                } catch (Exception e) {
                    NMMartLogger.logError("CategoryAdapter.java", "CategoryViewHolder click", e.getMessage());
                }
            });
        }

        // Bind data to views
        void bind(Category category) {
            try {
                if (category == null) return;
                tvCategoryName.setText(category.getName() != null ? category.getName() : "");
                if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
                    Glide.with(itemView.getContext())
                            .load(category.getImageUrl())
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground)
                            .into(ivCategoryIcon);
                } else {
                    ivCategoryIcon.setImageResource(R.drawable.ic_launcher_foreground);
                }
            } catch (Exception e) {
                NMMartLogger.logError("CategoryAdapter.java", "bind", e.getMessage());
            }
        }
    }
}
