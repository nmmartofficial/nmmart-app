package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.PaymentMethod;

import java.util.List;

public class PaymentMethodAdapter extends RecyclerView.Adapter<PaymentMethodAdapter.ViewHolder> {
    private Context context;
    private List<PaymentMethod> paymentMethods;
    private int selectedPosition = 0;
    private OnPaymentMethodSelectedListener listener;

    public interface OnPaymentMethodSelectedListener {
        void onPaymentMethodSelected(PaymentMethod method);
    }

    public PaymentMethodAdapter(Context context, List<PaymentMethod> paymentMethods, OnPaymentMethodSelectedListener listener) {
        this.context = context;
        this.paymentMethods = paymentMethods;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payment_method, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PaymentMethod method = paymentMethods.get(position);
        holder.ivPaymentIcon.setImageResource(method.iconRes);
        holder.tvPaymentName.setText(method.name);

        if (method.isComingSoon) {
            holder.tvPaymentStatus.setText("Coming Soon");
            holder.tvPaymentStatus.setTextColor(0xFFFF9800);
        } else {
            holder.tvPaymentStatus.setText("Available");
            holder.tvPaymentStatus.setTextColor(0xFF4CAF50);
        }

        holder.rbPayment.setChecked(position == selectedPosition);

        if (position == selectedPosition) {
            holder.card.setStrokeColor(context.getResources().getColor(R.color.orange_primary));
        } else {
            holder.card.setStrokeColor(0x00000000);
        }

        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onPaymentMethodSelected(method);
            }
        });
    }

    @Override
    public int getItemCount() {
        return paymentMethods.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        ImageView ivPaymentIcon;
        TextView tvPaymentName, tvPaymentStatus;
        RadioButton rbPayment;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (MaterialCardView) itemView;
            ivPaymentIcon = itemView.findViewById(R.id.ivPaymentIcon);
            tvPaymentName = itemView.findViewById(R.id.tvPaymentName);
            tvPaymentStatus = itemView.findViewById(R.id.tvPaymentStatus);
            rbPayment = itemView.findViewById(R.id.rbPayment);
        }
    }
}
