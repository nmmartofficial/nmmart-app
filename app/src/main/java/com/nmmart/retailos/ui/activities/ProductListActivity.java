package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.databinding.ActivityProductListBinding;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.adapters.ShimmerAdapter;
import com.nmmart.retailos.ui.viewmodels.ProductListViewModel;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends BaseActivity implements ProductListAdapter.OnProductClickListener {

    private ActivityProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductListAdapter adapter;
    private ShimmerAdapter shimmerAdapter;
    private List<Product> productList = new ArrayList<>();
    private String categoryName, searchQuery, brandName;
    private SessionManager sessionManager;
    private int selectedSortPosition = 0;

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
        adapter = new ProductListAdapter(this);
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
        binding.sortingLayout.setOnClickListener(v -> showSortBottomSheet());
    }
    
    private void showSortBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_sort_filter, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        RadioGroup radioGroupSort = bottomSheetView.findViewById(R.id.radioGroupSort);
        
        // Set current selection
        switch (selectedSortPosition) {
            case 0: radioGroupSort.check(R.id.radioRelevance); break;
            case 1: radioGroupSort.check(R.id.radioPriceLowHigh); break;
            case 2: radioGroupSort.check(R.id.radioPriceHighLow); break;
            case 3: radioGroupSort.check(R.id.radioDiscount); break;
        }
        
        radioGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioRelevance) selectedSortPosition = 0;
            else if (checkedId == R.id.radioPriceLowHigh) selectedSortPosition = 1;
            else if (checkedId == R.id.radioPriceHighLow) selectedSortPosition = 2;
            else if (checkedId == R.id.radioDiscount) selectedSortPosition = 3;
        });
        
        bottomSheetView.findViewById(R.id.btnApplySort).setOnClickListener(v -> {
            viewModel.sortProducts(selectedSortPosition);
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.show();
    }

    private void fetchData() {
        if (!isNetworkAvailable()) {
            showErrorState();
            return;
        }
        
        hideErrorState();
        if (searchQuery != null && !searchQuery.isEmpty()) {
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
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
