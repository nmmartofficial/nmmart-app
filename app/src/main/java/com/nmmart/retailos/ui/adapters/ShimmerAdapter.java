package com.nmmart.retailos.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ShimmerAdapter extends RecyclerView.Adapter<ShimmerAdapter.ViewHolder> {
    private int itemCount;
    private int layoutRes;

    public ShimmerAdapter(int itemCount, int layoutRes) {
        this.itemCount = itemCount;
        this.layoutRes = layoutRes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // No binding needed for shimmer
    }

    @Override
    public int getItemCount() {
        return itemCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
