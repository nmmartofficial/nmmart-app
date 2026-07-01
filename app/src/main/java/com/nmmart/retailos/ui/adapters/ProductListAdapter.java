package com.nmmart.retailos.ui.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.WishlistManager;
import com.nmmart.retailos.databinding.ItemLoadingBinding;
import com.nmmart.retailos.databinding.ItemProductBinding;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.utils.PriceUtils;

import java.util.ArrayList;
import java.util.List;

public class ProductListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int VIEW_TYPE_PRODUCT = 1;
    public static final int VIEW_TYPE_LOADING = 0;
    private final List<Product> productList = new ArrayList<>();
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
        if (productList != null) {
            this.productList.addAll(productList);
        }
        this.onProductClickListener = onProductClickListener;
    }

    public void setOnCartUpdateListener(OnCartUpdateListener listener) {
        this.onCartUpdateListener = listener;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.onProductClickListener = listener;
    }

    public void setProducts(List<Product> newProducts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProductDiffCallback(this.productList, newProducts));
        this.productList.clear();
        if (newProducts != null) {
            this.productList.addAll(newProducts);
        }
        diffResult.dispatchUpdatesTo(this);
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
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProductViewHolder && position < productList.size()) {
            ((ProductViewHolder) holder).bind(productList.get(position));
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding binding;

        ProductViewHolder(ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            
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
                    CartManager cartManager = CartManager.getInstance(context);
                    if (cartManager.addToCart(product)) {
                        updateQtyUI(1);
                        Toast.makeText(context, context.getString(R.string.added_to_cart, product.getName()), Toast.LENGTH_SHORT).show();
                        if (onCartUpdateListener != null) {
                            onCartUpdateListener.onCartUpdated();
                        }
                    } else {
                        Toast.makeText(context, "Cannot add more. Only " + product.getStock() + " in stock.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            binding.btnIncrease.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = productList.get(position);
                    CartManager cartManager = CartManager.getInstance(context);
                    int cartQty = cartManager.getQuantity(product);
                    if (cartQty < product.getStock()) {
                        cartManager.updateQuantity(product, cartQty + 1);
                        updateQtyUI(cartQty + 1);
                        if (onCartUpdateListener != null) {
                            onCartUpdateListener.onCartUpdated();
                        }
                    } else {
                        Toast.makeText(context, "Only " + product.getStock() + " in stock!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            binding.btnDecrease.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = productList.get(position);
                    CartManager cartManager = CartManager.getInstance(context);
                    int cartQty = cartManager.getQuantity(product);
                    if (cartQty > 1) {
                        cartManager.updateQuantity(product, cartQty - 1);
                        updateQtyUI(cartQty - 1);
                    } else {
                        cartManager.removeFromCart(product);
                        updateQtyUI(0);
                        Toast.makeText(context, product.getName() + " removed from cart", Toast.LENGTH_SHORT).show();
                    }
                    if (onCartUpdateListener != null) {
                        onCartUpdateListener.onCartUpdated();
                    }
                }
            });

            binding.ivWishlist.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Product product = productList.get(position);
                    WishlistManager.getInstance(context).toggleWishlist(product);
                    notifyItemChanged(position);
                }
            });
        }

        private void updateQtyUI(int qty) {
            if (qty > 0) {
                binding.tvQuantity.setText(String.valueOf(qty));
                binding.btnAddToCart.setVisibility(View.GONE);
                binding.btnDecrease.setVisibility(View.VISIBLE);
                binding.tvQuantity.setVisibility(View.VISIBLE);
                binding.btnIncrease.setVisibility(View.VISIBLE);
            } else {
                binding.btnAddToCart.setVisibility(View.VISIBLE);
                binding.btnDecrease.setVisibility(View.GONE);
                binding.tvQuantity.setVisibility(View.GONE);
                binding.btnIncrease.setVisibility(View.GONE);
            }
        }

        void bind(Product product) {
            binding.tvProductName.setText(product.getName());
            binding.tvUnit.setText(product.unit != null ? product.unit : "1 pcs");

            boolean isInWishlist = WishlistManager.getInstance(context).isInWishlist(product.getId());
            binding.ivWishlist.setImageResource(isInWishlist ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off);
            if (isInWishlist) binding.ivWishlist.setColorFilter(android.graphics.Color.parseColor("#FFC107"));
            else binding.ivWishlist.clearColorFilter();

            if (product.getMrp() > product.getNmPrice()) {
                binding.tvMrp.setText(PriceUtils.formatPrice(product.getMrp()));
                binding.tvMrp.setPaintFlags(binding.tvMrp.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                binding.tvMrp.setVisibility(View.VISIBLE);
            } else {
                binding.tvMrp.setVisibility(View.GONE);
            }

            binding.tvNmPrice.setText(PriceUtils.formatPrice(product.getNmPrice()));
            double discountPercent = product.getDiscountPercent();
            if (discountPercent > 0) {
                binding.cardDiscount.setVisibility(View.VISIBLE);
                binding.tvDiscountBadge.setText(String.format("%.0f%% OFF", discountPercent));
            } else {
                binding.cardDiscount.setVisibility(View.GONE);
            }

            if (product.getStock() > 0 && product.getStock() <= 5) {
                binding.tvInventoryAlert.setVisibility(View.VISIBLE);
                binding.tvInventoryAlert.setText("Only " + (int)product.getStock() + " left!");
            } else {
                binding.tvInventoryAlert.setVisibility(View.GONE);
            }

            int cartQty = CartManager.getInstance(context).getQuantity(product);
            updateQtyUI(cartQty);

            if (product.getStock() > 0) {
                binding.btnAddToCart.setEnabled(true);
                binding.btnAddToCart.setText("ADD");
            } else {
                binding.btnAddToCart.setEnabled(false);
                binding.btnAddToCart.setText("OFF");
                binding.btnIncrease.setEnabled(false);
                binding.btnDecrease.setEnabled(false);
            }

            Glide.with(context)
                    .load(product.getImageUrl())
                    .placeholder(R.drawable.ic_grocery_bag)
                    .error(R.drawable.ic_grocery_bag)
                    .into(binding.ivProduct);
        }
    }

    static class LoadingViewHolder extends RecyclerView.ViewHolder {
        LoadingViewHolder(ItemLoadingBinding binding) {
            super(binding.getRoot());
        }
    }

    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<Product> oldList;
        private final List<Product> newList;

        public ProductDiffCallback(List<Product> oldList, List<Product> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList != null ? oldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return newList != null ? newList.size() : 0;
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Product oldProduct = oldList.get(oldItemPosition);
            Product newProduct = newList.get(newItemPosition);
            return oldProduct.getName().equals(newProduct.getName()) &&
                   oldProduct.getNmPrice() == newProduct.getNmPrice() &&
                   oldProduct.getStock() == newProduct.getStock() &&
                   oldProduct.unit != null && oldProduct.unit.equals(newProduct.unit);
        }
    }
}
