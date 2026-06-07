package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nmmart.retailos.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    private Context context;
    private List<String> timeSlots;
    private int selectedPosition = 0;
    private OnTimeSlotSelectedListener listener;

    public interface OnTimeSlotSelectedListener {
        void onTimeSlotSelected(String timeSlot);
    }

    public TimeSlotAdapter(Context context, List<String> timeSlots, OnTimeSlotSelectedListener listener) {
        this.context = context;
        this.timeSlots = timeSlots;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String timeSlot = timeSlots.get(position);
        holder.tvTime.setText(timeSlot);

        com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) holder.itemView;

        if (selectedPosition == position) {
            cardView.setCardBackgroundColor(context.getResources().getColor(R.color.orange_primary));
            holder.tvTime.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white));
            holder.tvTime.setTextColor(context.getResources().getColor(android.R.color.black));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onTimeSlotSelected(timeSlot);
            }
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
