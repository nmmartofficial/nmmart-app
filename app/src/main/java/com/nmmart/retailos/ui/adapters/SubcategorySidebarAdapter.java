package com.nmmart.retailos.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Category;

import java.util.ArrayList;
import java.util.List;

public class SubcategorySidebarAdapter extends RecyclerView.Adapter<SubcategorySidebarAdapter.SubcategoryViewHolder> {

    private List<Category> subcategories;
    private int selectedPosition = 0;
    private OnSubcategoryClickListener listener;

    public interface OnSubcategoryClickListener {
        void onSubcategoryClick(Category subcategory, int position);
    }

    public SubcategorySidebarAdapter(List<Category> subcategories, OnSubcategoryClickListener listener) {
        this.subcategories = subcategories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SubcategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_subcategory_sidebar, parent, false);
        return new SubcategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubcategoryViewHolder holder, int position) {
        Category subcategory = subcategories.get(position);
        holder.tvName.setText(subcategory.getName());

        if (position == selectedPosition) {
            holder.itemView.setBackgroundColor(0xFFFFF3E0); // light orange
            holder.tvName.setTextColor(0xFFFF8C00); // orange primary
        } else {
            holder.itemView.setBackgroundColor(0xFFFFFFFF); // white
            holder.tvName.setTextColor(0xFF000000); // black
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onSubcategoryClick(subcategory, selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return subcategories.size();
    }

    public void updateSubcategories(List<Category> subcategories) {
        this.subcategories = subcategories;
        notifyDataSetChanged();
    }

    static class SubcategoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        public SubcategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvSubcategoryName);
        }
    }
}
