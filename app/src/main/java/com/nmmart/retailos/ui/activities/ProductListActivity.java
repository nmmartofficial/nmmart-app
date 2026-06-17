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
import com.nmmart.retailos.models.Category;
import com.nmmart.retailos.models.Product;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.adapters.ShimmerAdapter;
import com.nmmart.retailos.ui.adapters.SubcategorySidebarAdapter;
import com.nmmart.retailos.ui.viewmodels.ProductListViewModel;
import com.nmmart.retailos.utils.NetworkUtils;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

public class ProductListActivity extends BaseActivity implements ProductListAdapter.OnProductClickListener {

    private ActivityProductListBinding binding;
    private ProductListViewModel viewModel;
    private ProductListAdapter adapter;
    private ShimmerAdapter shimmerAdapter;
    private SubcategorySidebarAdapter subcategoryAdapter;
    private final List<Product> productList = new ArrayList<>();
    private final List<Category> subcategoryList = new ArrayList<>();
    private String categoryName, categoryId, searchQuery, brandId;
    private int selectedSortPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityProductListBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            viewModel = new ViewModelProvider(this).get(ProductListViewModel.class);
            
            categoryName = getIntent().getStringExtra("CATEGORY_NAME");
            categoryId = getIntent().getStringExtra("CATEGORY_ID");
            searchQuery = getIntent().getStringExtra("SEARCH_QUERY");
            brandId = getIntent().getStringExtra("BRAND_ID");

            setupToolbar();
            setupRecyclerView();
            setupObservers();
            setupListeners();
            
            fetchData();
            if (categoryId != null) {
                viewModel.fetchSubcategories(categoryId);
            }
        } catch (Exception e) {
            logError("Error in onCreate", e);
            finish();
        }
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        String brandNameFromIntent = getIntent().getStringExtra("BRAND_NAME");
        if (getSupportActionBar() != null) {
            if (categoryName != null) {
                getSupportActionBar().setTitle(categoryName);
            } else if (brandNameFromIntent != null) {
                getSupportActionBar().setTitle(brandNameFromIntent);
            } else if (searchQuery != null) {
                getSupportActionBar().setTitle("Search: " + searchQuery);
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> onBackPressed());
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

        binding.rvSubcategories.setLayoutManager(new LinearLayoutManager(this));
        subcategoryAdapter = new SubcategorySidebarAdapter(subcategoryList, (subcategory, position) -> {
            viewModel.fetchProductsBySubcategory(subcategory.getId());
        });
        binding.rvSubcategories.setAdapter(subcategoryAdapter);
        
        binding.rvProducts.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    
                    Boolean isLoading = viewModel.getIsLoading().getValue();
                    Boolean isLoadingMore = viewModel.getIsLoadingMore().getValue();
                    
                    if ((isLoading == null || !isLoading) && (isLoadingMore == null || !isLoadingMore) && !viewModel.isLastPage()) {
                        if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                            viewModel.loadMoreProducts();
                        }
                    }
                }
            }
        });
        
        binding.swipeRefreshLayout.setOnRefreshListener(this::fetchData);
        binding.btnRetry.setOnClickListener(v -> fetchData());
    }
    
    private void showErrorState(String message) {
        binding.swipeRefreshLayout.setRefreshing(false);
        binding.errorLayout.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
        binding.btnRetry.setVisibility(View.VISIBLE);
        binding.rvProducts.setVisibility(View.GONE);
    }
    
    private void hideErrorState() {
        binding.errorLayout.setVisibility(View.GONE);
        binding.rvProducts.setVisibility(View.VISIBLE);
    }

    private void setupObservers() {
        viewModel.getProducts().observe(this, products -> {
            if (products != null) {
                if (products.isEmpty()) {
                    showErrorState(getString(R.string.no_products_found));
                    binding.btnRetry.setVisibility(View.GONE);
                } else {
                    hideErrorState();
                    adapter.setProducts(products);
                }
            }
            binding.swipeRefreshLayout.setRefreshing(false);
        });

        viewModel.getSubcategories().observe(this, categories -> {
            if (categories != null && !categories.isEmpty()) {
                subcategoryList.clear();
                subcategoryList.addAll(categories);
                subcategoryAdapter.updateSubcategories(subcategoryList);
                binding.rvSubcategories.setVisibility(View.VISIBLE);
            } else {
                binding.rvSubcategories.setVisibility(View.GONE);
            }
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null && isLoading) {
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
                showErrorState(msg);
            }
        });
    }

    private void setupListeners() {
        binding.sortingLayout.setOnClickListener(v -> showSortBottomSheet());
    }
    
    private boolean filterInStockOnly = false;

    private void showSortBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View bottomSheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_sort_filter, null);
        bottomSheetDialog.setContentView(bottomSheetView);
        
        RadioGroup radioGroupSort = bottomSheetView.findViewById(R.id.radioGroupSort);
        android.widget.CheckBox checkInStock = bottomSheetView.findViewById(R.id.checkInStock);
        
        switch (selectedSortPosition) {
            case 0: radioGroupSort.check(R.id.radioRelevance); break;
            case 1: radioGroupSort.check(R.id.radioPriceLowHigh); break;
            case 2: radioGroupSort.check(R.id.radioPriceHighLow); break;
            case 3: radioGroupSort.check(R.id.radioDiscount); break;
        }

        checkInStock.setChecked(filterInStockOnly);
        
        radioGroupSort.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioRelevance) selectedSortPosition = 0;
            else if (checkedId == R.id.radioPriceLowHigh) selectedSortPosition = 1;
            else if (checkedId == R.id.radioPriceHighLow) selectedSortPosition = 2;
            else if (checkedId == R.id.radioDiscount) selectedSortPosition = 3;
        });
        
        bottomSheetView.findViewById(R.id.btnApplySort).setOnClickListener(v -> {
            filterInStockOnly = checkInStock.isChecked();
            viewModel.setFilterInStockOnly(filterInStockOnly);
            viewModel.applySortAndFilter(selectedSortPosition);
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.show();
    }

    private void fetchData() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            showErrorState(getString(R.string.network_error));
            return;
        }
        
        hideErrorState();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            viewModel.searchProducts(searchQuery);
        } else if (brandId != null) {
            viewModel.fetchProductsByBrand(brandId);
        } else {
            viewModel.fetchProducts(categoryId);
        }
    }

    @Override
    public void onProductClick(Product product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("PRODUCT", product);
        startActivity(intent);
    }
}
