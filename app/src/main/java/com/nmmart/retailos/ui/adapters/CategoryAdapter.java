package com.nmmart.retailos.ui.adapters;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private final List<Category> categories = new ArrayList<>();
    private final OnCategoryClickListener listener;
    private ThemeManager themeManager;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(List<Category> categories, OnCategoryClickListener listener) {
        if (categories != null) {
            this.categories.addAll(categories);
        }
        this.listener = listener;
    }

    public void setCategories(List<Category> newCategories) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new CategoryDiffCallback(this.categories, newCategories));
        this.categories.clear();
        if (newCategories != null) {
            this.categories.addAll(newCategories);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        if (themeManager == null) {
            themeManager = ThemeManager.getInstance(parent.getContext());
        }
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        if (position >= 0 && position < categories.size()) {
            holder.bind(categories.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    class CategoryViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivCategoryIcon;
        final TextView tvCategoryName;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
            tvCategoryName = itemView.findViewById(R.id.tvCategoryName);

            // Apply theme shape to category icon
            applyThemeToIcon();

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCategoryClick(categories.get(position));
                }
            });
        }

        private void applyThemeToIcon() {
            if (themeManager != null) {
                GradientDrawable shapeDrawable = themeManager.getShapeDrawable(
                        themeManager.getCategoryShape(),
                        Color.parseColor("#F5F5F5"),
                        16
                );
                ivCategoryIcon.setBackground(shapeDrawable);
            }
        }

        void bind(Category category) {
            tvCategoryName.setText(category.getName());
            if (themeManager != null) {
                tvCategoryName.setTextColor(themeManager.getTextColorPrimary());
            }
            Glide.with(itemView.getContext())
                    .load(category.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(ivCategoryIcon);
        }
    }

    private static class CategoryDiffCallback extends DiffUtil.Callback {
        private final List<Category> oldList;
        private final List<Category> newList;

        public CategoryDiffCallback(List<Category> oldList, List<Category> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldPos, int newPos) {
            return oldList.get(oldPos).getId().equals(newList.get(newPos).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldPos, int newPos) {
            Category oldItem = oldList.get(oldPos);
            Category newItem = newList.get(newPos);
            return oldItem.getName().equals(newItem.getName()) &&
                   String.valueOf(oldItem.getImageUrl()).equals(String.valueOf(newItem.getImageUrl()));
        }
    }
}
