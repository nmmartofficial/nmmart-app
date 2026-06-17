package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.WalletTransaction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {
    private Context context;
    private List<WalletTransaction> transactions = new ArrayList<>();

    public WalletTransactionAdapter(Context context) {
        this.context = context;
    }

    public void updateTransactions(List<WalletTransaction> transactions) {
        this.transactions = transactions;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction tx = transactions.get(position);
        holder.tvDescription.setText(tx.getDescription());
        if (tx.getType().equals("credit")) {
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.green_success));
            holder.tvAmount.setText("+₹" + String.format("%.2f", tx.getAmount()));
        } else {
            holder.tvAmount.setTextColor(context.getResources().getColor(R.color.red_error));
            holder.tvAmount.setText("-₹" + String.format("%.2f", tx.getAmount()));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String dateStr = sdf.format(new Date(tx.getTimestamp()));
        holder.tvDate.setText(dateStr);
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tvDescription;
        TextView tvAmount;
        TextView tvDate;

        public ViewHolder(View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            tvDescription = itemView.findViewById(R.id.tvTransactionDescription);
            tvAmount = itemView.findViewById(R.id.tvTransactionAmount);
            tvDate = itemView.findViewById(R.id.tvTransactionDate);
        }
    }
}
