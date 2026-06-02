package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.nmmart.retailos.R;
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
    private android.widget.Button startShoppingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history); // Reusing history layout for simplicity

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("My Wishlist");
        }

        rvWishlist = findViewById(R.id.rvOrders); // ID from activity_order_history
        emptyLayout = findViewById(R.id.emptyOrdersLayout);
        emptyText = emptyLayout.findViewById(R.id.tvEmptyState);
        startShoppingBtn = emptyLayout.findViewById(R.id.btnStartShopping);
        
        // Customize empty state for Wishlist
        if (emptyText != null) {
            emptyText.setText("Wishlist is empty!");
        }
        if (startShoppingBtn != null) {
            startShoppingBtn.setText("BROWSE PRODUCTS");
            startShoppingBtn.setOnClickListener(v -> {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
        
        setupRecyclerView();
        loadWishlist();
    }

    private void setupRecyclerView() {
        adapter = new ProductListAdapter(new ArrayList<>(), product -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        rvWishlist.setLayoutManager(new GridLayoutManager(this, 2));
        rvWishlist.setAdapter(adapter);
    }

    private void loadWishlist() {
        List<Product> items = new ArrayList<>(WishlistManager.getInstance(this).getWishlistItems().values());
        if (items.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            rvWishlist.setVisibility(View.GONE);
        } else {
            emptyLayout.setVisibility(View.GONE);
            rvWishlist.setVisibility(View.VISIBLE);
            adapter.setProducts(items);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}