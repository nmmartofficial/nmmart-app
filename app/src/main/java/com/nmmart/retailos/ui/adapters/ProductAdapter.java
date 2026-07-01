package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.activities.ProductDetailActivity;
import com.nmmart.retailos.utils.PriceUtils;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private List<Product> productList;
    private Context context;
    private int cornerRadius;

    public ProductAdapter(List<Product> productList, Context context) {
        this.productList = productList;
        this.context = context;
        
        // Get shape from session
        SessionManager sessionManager = new SessionManager(context);
        String shape = sessionManager.getCategoryShape();
        int radius = 16;
        switch (shape) {
            case "sharp": radius = 0; break;
            case "square":
            case "rounded": radius = 16; break;
            case "soft": radius = 32; break;
            case "pill":
            case "circle": radius = 100; break;
            default: radius = 16; break;
        }
        this.cornerRadius = dpToPx(radius);
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_horizontal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getName());
        holder.tvPrice.setText(PriceUtils.formatPrice(product.getNmPrice()));
        
        // Apply corner radius
        if (holder.itemView instanceof MaterialCardView) {
            ((MaterialCardView) holder.itemView).setRadius(cornerRadius);
        }

        if (holder.tvMrp != null) {
            if (product.getMrp() > product.getNmPrice()) {
                holder.tvMrp.setText(PriceUtils.formatPrice(product.getMrp()));
                holder.tvMrp.setPaintFlags(holder.tvMrp.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvMrp.setVisibility(View.VISIBLE);
            } else {
                holder.tvMrp.setVisibility(View.GONE);
            }
        }

        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.ic_grocery_bag)
                    .error(R.drawable.ic_grocery_bag)
                    .fallback(R.drawable.ic_grocery_bag)
                    .dontAnimate()
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_grocery_bag);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvMrp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.ivProduct);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvMrp = itemView.findViewById(R.id.tvMrp);
        }
    }
}
