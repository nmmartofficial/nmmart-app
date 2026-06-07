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

public class DateAdapter extends RecyclerView.Adapter<DateAdapter.ViewHolder> {

    private Context context;
    private List<String> dates;
    private int selectedPosition = 0;
    private OnDateSelectedListener listener;

    public interface OnDateSelectedListener {
        void onDateSelected(String date);
    }

    public DateAdapter(Context context, List<String> dates, OnDateSelectedListener listener) {
        this.context = context;
        this.dates = dates;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_date, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String date = dates.get(position);
        holder.tvDate.setText(date.split(" ")[0]);
        holder.tvDay.setText(date.split(" ")[1]);

        com.google.android.material.card.MaterialCardView cardView = (com.google.android.material.card.MaterialCardView) holder.itemView;

        if (selectedPosition == position) {
            cardView.setCardBackgroundColor(context.getResources().getColor(R.color.orange_primary));
            holder.tvDate.setTextColor(context.getResources().getColor(android.R.color.white));
            holder.tvDay.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            cardView.setCardBackgroundColor(context.getResources().getColor(android.R.color.white));
            holder.tvDate.setTextColor(context.getResources().getColor(android.R.color.black));
            holder.tvDay.setTextColor(context.getResources().getColor(android.R.color.darker_gray));
        }

        holder.itemView.setOnClickListener(v -> {
            selectedPosition = position;
            notifyDataSetChanged();
            if (listener != null) {
                listener.onDateSelected(date);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvDay;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvDay = itemView.findViewById(R.id.tvDay);
        }
    }
}
