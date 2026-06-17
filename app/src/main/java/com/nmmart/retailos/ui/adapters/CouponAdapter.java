package com.nmmart.retailos.ui.adapters;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Coupon;

import java.util.List;

public class CouponAdapter extends RecyclerView.Adapter<CouponAdapter.ViewHolder> {

    private Context context;
    private List<Coupon> coupons;

    public CouponAdapter(Context context, List<Coupon> coupons) {
        this.context = context;
        this.coupons = coupons;
    }

    public void updateCoupons(List<Coupon> coupons) {
        this.coupons = coupons;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_coupon, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Coupon coupon = coupons.get(position);
        holder.tvCouponCode.setText(coupon.code);
        holder.tvCouponDesc.setText(coupon.description);
        holder.tvCouponDiscount.setText(String.valueOf((int) coupon.discountAmount));
        holder.tvCouponMinOrder.setText("Min Order: ₹" + String.format("%.0f", coupon.minOrderValue));

        holder.btnCopyCode.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Coupon Code", coupon.code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Coupon code copied: " + coupon.code, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return coupons.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCouponCode, tvCouponDesc, tvCouponDiscount, tvCouponMinOrder;
        Button btnCopyCode;

        public ViewHolder(View itemView) {
            super(itemView);
            tvCouponCode = itemView.findViewById(R.id.tvCouponCode);
            tvCouponDesc = itemView.findViewById(R.id.tvCouponDesc);
            tvCouponDiscount = itemView.findViewById(R.id.tvCouponDiscount);
            tvCouponMinOrder = itemView.findViewById(R.id.tvCouponMinOrder);
            btnCopyCode = itemView.findViewById(R.id.btnCopyCode);
        }
    }
}
