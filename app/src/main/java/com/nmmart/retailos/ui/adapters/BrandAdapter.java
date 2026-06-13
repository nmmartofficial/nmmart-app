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
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.utils.ThemeManager;

import java.util.ArrayList;
import java.util.List;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {

    private final List<Brand> brands = new ArrayList<>();
    private final OnBrandClickListener listener;
    private ThemeManager themeManager;

    public interface OnBrandClickListener {
        void onBrandClick(Brand brand);
    }

    public BrandAdapter(OnBrandClickListener listener) {
        this.listener = listener;
    }

    public void setBrands(List<Brand> newBrands) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BrandDiffCallback(this.brands, newBrands));
        this.brands.clear();
        if (newBrands != null) {
            this.brands.addAll(newBrands);
        }
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        if (themeManager == null) {
            themeManager = ThemeManager.getInstance(parent.getContext());
        }
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        if (position >= 0 && position < brands.size()) {
            holder.bind(brands.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return brands.size();
    }

    class BrandViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivBrandLogo;
        final TextView tvBrandName;

        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);

            // Apply theme shape to brand logo
            applyThemeToLogo();

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBrandClick(brands.get(position));
                }
            });
        }

        private void applyThemeToLogo() {
            if (themeManager != null) {
                GradientDrawable shapeDrawable = themeManager.getShapeDrawable(
                        themeManager.getBrandShape(),
                        Color.parseColor("#FFFFFF"),
                        12
                );
                ivBrandLogo.setBackground(shapeDrawable);
            }
        }

        void bind(Brand brand) {
            tvBrandName.setText(brand.getName());
            if (themeManager != null) {
                tvBrandName.setTextColor(themeManager.getTextColorPrimary());
            }
            Glide.with(itemView.getContext())
                    .load(brand.getLogoUrl())
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(ivBrandLogo);
        }
    }

    private static class BrandDiffCallback extends DiffUtil.Callback {
        private final List<Brand> oldList;
        private final List<Brand> newList;

        public BrandDiffCallback(List<Brand> oldList, List<Brand> newList) {
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
            Brand oldItem = oldList.get(oldPos);
            Brand newItem = newList.get(newPos);
            return oldItem.getName().equals(newItem.getName()) &&
                   String.valueOf(oldItem.getLogoUrl()).equals(String.valueOf(newItem.getLogoUrl()));
        }
    }
}
