package com.nmmart.retailos.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.nmmart.retailos.R;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryAdapter extends RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder> {
    private List<String> historyList;
    private OnSearchHistoryClickListener listener;

    public interface OnSearchHistoryClickListener {
        void onHistoryClick(String query);
    }

    public SearchHistoryAdapter(List<String> historyList, OnSearchHistoryClickListener listener) {
        this.historyList = historyList != null ? historyList : new ArrayList<>();
        this.listener = listener;
    }

    public void updateHistory(List<String> newHistory) {
        this.historyList = newHistory != null ? newHistory : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String query = historyList.get(position);
        holder.tvHistoryQuery.setText(query);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onHistoryClick(query);
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHistoryQuery;
        ViewHolder(View itemView) {
            super(itemView);
            tvHistoryQuery = itemView.findViewById(R.id.tvHistoryQuery);
        }
    }
}
