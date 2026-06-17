package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.WishlistManager;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {
    private ProductListAdapter adapter;
    private RecyclerView rvWishlist;
    private View emptyLayout;
    private TextView emptyText;
    private com.google.android.material.button.MaterialButton startShoppingBtn;
    private WishlistManager wishlistManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        wishlistManager = WishlistManager.getInstance(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvWishlist = findViewById(R.id.rvWishlist);
        emptyLayout = findViewById(R.id.emptyWishlistLayout);
        emptyText = emptyLayout.findViewById(R.id.tvEmptyState);
        startShoppingBtn = emptyLayout.findViewById(R.id.btnStartShopping);
        
        startShoppingBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        
        setupRecyclerView();
        loadWishlist();
    }

    private void setupRecyclerView() {
        adapter = new ProductListAdapter(this);
        adapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        adapter.setOnCartUpdateListener(this::updateCartBadge);
        rvWishlist.setLayoutManager(new GridLayoutManager(this, 2));
        rvWishlist.setAdapter(adapter);
    }

    private void loadWishlist() {
        List<Product> items = new ArrayList<>(wishlistManager.getWishlistItems().values());
        if (items.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            rvWishlist.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            rvWishlist.setVisibility(View.VISIBLE);
            adapter.setProducts(items);
        }
    }

    private void updateCartBadge() {
        // If you want to update a badge, you can handle it here,
        // but for now, we'll just ensure the cart state is saved
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist(); // Refresh wishlist when returning to this activity
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}