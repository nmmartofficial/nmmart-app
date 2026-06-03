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
import com.nmmart.retailos.models.Brand;

import java.util.ArrayList;
import java.util.List;

public class BrandAdapter extends RecyclerView.Adapter<BrandAdapter.BrandViewHolder> {

    // Data & Listener
    private List<Brand> brands = new ArrayList<>();
    private OnBrandClickListener listener;

    // Click listener interface
    public interface OnBrandClickListener {
        void onBrandClick(Brand brand);
    }

    // Constructor
    public BrandAdapter(OnBrandClickListener listener) {
        this.listener = listener;
    }

    // Update brands data
    public void setBrands(List<Brand> brands) {
        this.brands = brands;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BrandViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_brand, parent, false);
        return new BrandViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BrandViewHolder holder, int position) {
        holder.bind(brands.get(position));
    }

    @Override
    public int getItemCount() {
        return brands.size();
    }

    // ViewHolder class
    class BrandViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBrandLogo;
        TextView tvBrandName;

        public BrandViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBrandLogo = itemView.findViewById(R.id.ivBrandLogo);
            tvBrandName = itemView.findViewById(R.id.tvBrandName);
            
            // Handle clicks in ViewHolder
            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onBrandClick(brands.get(position));
                }
            });
        }

        // Bind data to views
        void bind(Brand brand) {
            tvBrandName.setText(brand.getName());
            if (brand.getLogoUrl() != null && !brand.getLogoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(brand.getLogoUrl())
                        .placeholder(R.drawable.ic_placeholder)
                        .error(R.drawable.ic_placeholder)
                        .into(ivBrandLogo);
            } else {
                ivBrandLogo.setImageResource(R.drawable.ic_placeholder);
            }
        }
    }
}
