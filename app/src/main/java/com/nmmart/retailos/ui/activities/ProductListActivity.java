package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.databinding.ActivityProductListBinding;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.adapters.ShimmerAdapter;
import com.nmmart.retailos.ui.viewmodels.ProductListViewModel;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends BaseActivity implements ProductListAdapter.OnProductLongClickListener, ProductListAdapter.OnProductClickListener {

    private ActivityProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductListAdapter adapter;
    private ShimmerAdapter shimmerAdapter;
    private List<Product> productList = new ArrayList<>();
    private String categoryName, searchQuery, brandName;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);
        sessionManager = new SessionManager(this);
        
        categoryName = getIntent().getStringExtra("CATEGORY_NAME");
        searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
        brandName = getIntent().getStringExtra("BRAND_NAME");

        setupToolbar();
        setupRecyclerView();
        setupObservers();
        setupListeners();
        
        fetchData();
    }
    
    private boolean isNetworkAvailable() {
        android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        android.net.NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            if (categoryName != null) {
                getSupportActionBar().setTitle(categoryName);
            } else if (brandName != null) {
                getSupportActionBar().setTitle(brandName);
            } else {
                getSupportActionBar().setTitle("Search: " + searchQuery);
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.rvProducts.setLayoutManager(layoutManager);
        adapter = new ProductListAdapter(this, this);
        adapter.setOnProductClickListener(this);
        binding.rvProducts.setAdapter(adapter);
        
        binding.rvShimmer.setLayoutManager(new LinearLayoutManager(this));
        shimmerAdapter = new ShimmerAdapter(5, R.layout.item_shimmer_product);
        binding.rvShimmer.setAdapter(shimmerAdapter);
        
        // Setup scroll listener for load more
        binding.rvProducts.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy > 0) { // Only when scrolling down
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    Boolean isLoading = viewModel.getIsLoading().getValue();
                    Boolean isLoadingMore = viewModel.getIsLoadingMore().getValue();
                    
                    if ((isLoading == null || !isLoading) && 
                        (isLoadingMore == null || !isLoadingMore) && 
                        !viewModel.isLastPage()) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                            viewModel.loadMoreProducts();
                        }
                    }
                }
            }
        });
        
        // Setup SwipeRefreshLayout
        binding.swipeRefreshLayout.setColorSchemeResources(com.google.android.material.R.color.design_default_color_primary);
        binding.swipeRefreshLayout.setOnRefreshListener(() -> {
            if (isNetworkAvailable()) {
                binding.errorLayout.setVisibility(android.view.View.GONE);
                fetchData();
            } else {
                binding.swipeRefreshLayout.setRefreshing(false);
                showErrorState();
            }
        });
        
        // Retry button
        binding.btnRetry.setOnClickListener(v -> {
            if (isNetworkAvailable()) {
                binding.errorLayout.setVisibility(android.view.View.GONE);
                fetchData();
            }
        });
    }
    
    private void showErrorState() {
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.errorLayout.setVisibility(android.view.View.VISIBLE);
    }
    
    private void hideErrorState() {
        binding.errorLayout.setVisibility(android.view.View.GONE);
    }

    private void setupObservers() {
        viewModel.getProducts().observe(this, products -> {
            if (products != null) {
                hideErrorState();
                productList.clear();
                productList.addAll(products);
                adapter.setProducts(products);
                
                // Check if empty
                if (products.isEmpty()) {
                    binding.errorLayout.setVisibility(View.VISIBLE);
                    binding.tvError.setText("No products available!");
                    binding.btnRetry.setVisibility(View.GONE);
                } else {
                    binding.errorLayout.setVisibility(View.GONE);
                    binding.btnRetry.setVisibility(View.VISIBLE);
                }
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading) {
                binding.rvProducts.setVisibility(View.GONE);
                binding.rvShimmer.setVisibility(View.VISIBLE);
            } else {
                binding.rvShimmer.setVisibility(View.GONE);
                binding.rvProducts.setVisibility(View.VISIBLE);
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        
        viewModel.getIsLoadingMore().observe(this, isLoadingMore -> {
            adapter.setLoading(isLoadingMore != null && isLoadingMore);
        });

        viewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                showErrorState();
            }
        });
    }

    private void setupListeners() {
        binding.spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.sortProducts(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchData() {
        if (!isNetworkAvailable()) {
            showErrorState();
            return;
        }
        
        hideErrorState();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            viewModel.searchProducts(searchQuery);
        } else if (categoryName != null) {
            viewModel.fetchProductsByCategory(categoryName);
        } else if (brandName != null) {
            viewModel.fetchProductsByBrand(brandName);
        } else {
            viewModel.fetchProductsByCategory(null); // Load all
        }
    }
        if (!isNetworkAvailable()) {
            showErrorState();
            return;
        }
        if (searchQuery != null) {
            viewModel.searchProducts(searchQuery);
        } else if (brandName != null) {
            viewModel.fetchProductsByBrand(brandName);
        } else {
            viewModel.fetchProducts(categoryName);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT", product);
        startActivity(intent);
    }

    @Override
    public void onProductLongClick(Product product) {
        if (!sessionManager.isAdmin()) return;

        EditText etPrice = new EditText(this);
        etPrice.setHint("New Price");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Edit " + product.name)
                .setView(etPrice)
                .setPositiveButton("Update", (d, w) -> {
                    String price = etPrice.getText().toString();
                    if (!price.isEmpty()) viewModel.updatePrice(product, Double.parseDouble(price));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
