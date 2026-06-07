package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SelfCheckoutCartManager;
import com.nmmart.retailos.models.Product;

import java.util.ArrayList;
import java.util.List;

public class SelfCheckoutCartAdapter extends RecyclerView.Adapter<SelfCheckoutCartAdapter.ViewHolder> {
    private List<Product> items;
    private Context context;
    private SelfCheckoutCartManager cartManager;
    private OnCartUpdateListener listener;

    public interface OnCartUpdateListener {
        void onCartUpdated();
    }

    public SelfCheckoutCartAdapter(Context context, OnCartUpdateListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.cartManager = SelfCheckoutCartManager.getInstance(context);
        this.listener = listener;
    }

    public void setItems(List<Product> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = items.get(position);
        holder.tvProductName.setText(product.name);
        holder.tvProductUnit.setText(product.unit);
        holder.tvProductPrice.setText(String.format("₹%.2f", product.getNmPrice()));
        
        if (product.image_url != null && !product.image_url.isEmpty()) {
            Glide.with(context).load(product.image_url).into(holder.ivProduct);
        }
        
        int qty = cartManager.getQuantity(product.id);
        holder.tvQty.setText(String.valueOf(qty));
        
        holder.btnPlus.setOnClickListener(v -> {
            int newQty = qty + 1;
            if (product.getStock() > 0 && newQty <= product.getStock()) {
                cartManager.updateQuantity(product.id, newQty);
                holder.tvQty.setText(String.valueOf(newQty));
                if (listener != null) listener.onCartUpdated();
            }
        });
        
        holder.btnMinus.setOnClickListener(v -> {
            int newQty = qty - 1;
            if (newQty <= 0) {
                cartManager.removeItem(product.id);
                items.remove(position);
                notifyItemRemoved(position);
            } else {
                cartManager.updateQuantity(product.id, newQty);
                holder.tvQty.setText(String.valueOf(newQty));
            }
            if (listener != null) listener.onCartUpdated();
        });

        // Hide "Save for later" text
        holder.tvAction.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvProductName, tvProductUnit, tvProductPrice, tvQty, tvAction;
        ImageButton btnPlus, btnMinus;

        ViewHolder(View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivCartProduct);
            tvProductName = itemView.findViewById(R.id.tvCartProductName);
            tvProductUnit = itemView.findViewById(R.id.tvCartProductUnit);
            tvProductPrice = itemView.findViewById(R.id.tvCartProductPrice);
            tvQty = itemView.findViewById(R.id.tvQty);
            tvAction = itemView.findViewById(R.id.tvAction);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnMinus = itemView.findViewById(R.id.btnMinus);
        }
    }
}
