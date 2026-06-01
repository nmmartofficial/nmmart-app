package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.databinding.ItemLoadingBinding;
import com.nmmart.retailos.databinding.ItemProductBinding;
import com.nmmart.retailos.models.Product;
import java.util.ArrayList;
import java.util.List;

public class ProductListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_PRODUCT = 1;
    public static final int VIEW_TYPE_LOADING = 0;
    private List<Product> productList = new ArrayList<>();
    private boolean isLoading = false;
    private Context context;
    private OnProductClickListener onProductClickListener;
    private OnCartUpdateListener onCartUpdateListener;

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

    public interface OnCartUpdateListener {
        void onCartUpdated();
    }

    public ProductListAdapter(Context context) {
        this.context = context;
    }

    public ProductListAdapter(List<Product> productList, OnProductClickListener onProductClickListener) {
        this.productList = productList != null ? productList : new ArrayList<>();
        this.onProductClickListener = onProductClickListener;
    }

    public void setOnCartUpdateListener(OnCartUpdateListener listener) {
        this.onCartUpdateListener = listener;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.onProductClickListener = listener;
    }

    public void setProducts(List<Product> products) {
        this.productList = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setLoading(boolean loading) {
        if (isLoading != loading) {
            isLoading = loading;
            if (loading) {
                notifyItemInserted(productList.size());
            } else {
                notifyItemRemoved(productList.size());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (isLoading && position == productList.size()) {
            return VIEW_TYPE_LOADING;
        }
        return VIEW_TYPE_PRODUCT;
    }

    @Override
    public int getItemCount() {
        return productList.size() + (isLoading ? 1 : 0);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_LOADING) {
            ItemLoadingBinding binding = ItemLoadingBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false);
            return new LoadingViewHolder(binding);
        }

        ItemProductBinding binding = ItemProductBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        context = parent.getContext();
        return new ProductViewHolder(binding, context, productList, onProductClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProductViewHolder && position < productList.size()) {
            ((ProductViewHolder) holder).bind(productList.get(position));
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ItemProductBinding binding;
        private Context context;
        private List<Product> productList;
        private OnProductClickListener onProductClickListener;

        ProductViewHolder(ItemProductBinding binding, Context context, List<Product> productList, 
                          OnProductClickListener onProductClickListener) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = context;
            this.productList = productList;
            this.onProductClickListener = onProductClickListener;
            
            binding.getRoot().setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && onProductClickListener != null) {
                    onProductClickListener.onProductClick(productList.get(position));
                }
            });
            
            binding.btnAddToCart.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = productList.get(position);
                    com.nmmart.retailos.data.CartManager cartManager = com.nmmart.retailos.data.CartManager.getInstance(context);
                    if (cartManager.addToCart(product)) {
                        android.widget.Toast.makeText(context, product.name + " added to cart!", android.widget.Toast.LENGTH_SHORT).show();
                        // Notify listener that cart is updated
                        if (onCartUpdateListener != null) {
                            onCartUpdateListener.onCartUpdated();
                        }
                    } else {
                        android.widget.Toast.makeText(context, "Cannot add more. Only " + product.getStock() + " in stock.", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
            });

            binding.ivWishlist.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = productList.get(position);
                    com.nmmart.retailos.data.WishlistManager.getInstance(context).toggleWishlist(product);
                    notifyItemChanged(position);
                }
            });
        }

        void bind(Product product) {
            Glide.with(context).clear(binding.ivProduct);
            binding.tvProductName.setText(product.name != null ? product.name : "Product");
            binding.tvUnit.setText(product.unit != null ? product.unit : "1 pcs");

            // Wishlist icon update
            boolean isInWishlist = com.nmmart.retailos.data.WishlistManager.getInstance(context).isInWishlist(product.id);
            binding.ivWishlist.setImageResource(isInWishlist ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            if (isInWishlist) binding.ivWishlist.setColorFilter(android.graphics.Color.parseColor("#FFC107"));
            else binding.ivWishlist.clearColorFilter();

            if (product.getMrp() > product.getNmPrice()) {
                binding.tvMrp.setText(com.nmmart.retailos.utils.PriceUtils.formatPrice(product.getMrp()));
                binding.tvMrp.setPaintFlags(binding.tvMrp.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvMrp.setVisibility(View.VISIBLE);
            } else {
                binding.tvMrp.setVisibility(View.GONE);
            }

            binding.tvNmPrice.setText(com.nmmart.retailos.utils.PriceUtils.formatPrice(product.getNmPrice()));

            // Inventory Alert Logic
            if (product.getStock() > 0 && product.getStock() <= 5) {
                binding.tvInventoryAlert.setVisibility(View.VISIBLE);
                binding.tvInventoryAlert.setText("Only " + product.getStock() + " left!");
            } else {
                binding.tvInventoryAlert.setVisibility(View.GONE);
            }

            if (product.getStock() > 0) {
                binding.tvStock.setText("In Stock: " + product.getStock());
                binding.tvStock.setTextColor(Color.parseColor("#388E3C"));
                binding.btnAddToCart.setEnabled(true);
            } else {
                binding.tvStock.setText("Out of Stock");
                binding.tvStock.setTextColor(Color.RED);
                binding.btnAddToCart.setEnabled(false);
            }

            if (product.image_url != null && !product.image_url.isEmpty()) {
                Glide.with(context)
                        .load(product.image_url)
                        .placeholder(R.drawable.ic_grocery_bag)
                        .error(R.drawable.ic_grocery_bag)
                        .dontAnimate()
                        .into(binding.ivProduct);
            } else {
                binding.ivProduct.setImageResource(R.drawable.ic_grocery_bag);
            }
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(ItemLoadingBinding binding) {
            super(binding.getRoot());
        }
    }
}
