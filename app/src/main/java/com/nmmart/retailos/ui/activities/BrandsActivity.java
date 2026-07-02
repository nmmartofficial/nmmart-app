package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.databinding.ActivityBrandsBinding;
import com.nmmart.retailos.models.Brand;
import com.nmmart.retailos.ui.adapters.BrandAdapter;
import com.nmmart.retailos.ui.adapters.ProductListAdapter;
import com.nmmart.retailos.ui.viewmodels.MainViewModel;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BrandsActivity extends BaseActivity {

    private ActivityBrandsBinding binding;
    private BrandAdapter brandAdapter;
    private ProductListAdapter productAdapter;
    private final List<Brand> brandList = new ArrayList<>();
    private final List<com.nmmart.retailos.models.Product> productList = new ArrayList<>();
    private SupabaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityBrandsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SupabaseRepository();

        setupToolbar(binding.toolbar, getString(R.string.all_brands_label));
        setupRecyclerViews();
        setupListeners();
        loadBrands();
    }

    private void setupRecyclerViews() {
        binding.rvBrands.setLayoutManager(new LinearLayoutManager(this));
        brandAdapter = new BrandAdapter(this::openBrandProducts);
        binding.rvBrands.setAdapter(brandAdapter);

        binding.rvProducts.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        productAdapter = new ProductListAdapter(this);
        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(BrandsActivity.this, ProductDetailActivity.class);
            intent.putExtra("PRODUCT", product);
            startActivity(intent);
        });
        binding.rvProducts.setAdapter(productAdapter);
    }

    private void setupListeners() {
        binding.btnRetry.setOnClickListener(v -> loadBrands());
    }

    private void loadBrands() {
        showLoading();
        repository.getBrands(new Callback<List<Brand>>() {
            @Override
            public void onResponse(Call<List<Brand>> call, Response<List<Brand>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    brandList.clear();
                    brandList.addAll(response.body());
                    brandAdapter.setBrands(brandList);
                    showContent();
                } else {
                    showError(getString(R.string.error_loading_data));
                }
            }

            @Override
            public void onFailure(Call<List<Brand>> call, Throwable t) {
                showError(getString(R.string.network_error));
            }
        });
    }

    private void openBrandProducts(Brand brand) {
        if (brand == null || brand.getId() == null) return;
        Intent intent = new Intent(this, ProductListActivity.class);
        intent.putExtra("BRAND_ID", brand.getId());
        intent.putExtra("BRAND_NAME", brand.getName());
        startActivity(intent);
    }

    private void showLoading() {
        binding.progressLayout.setVisibility(View.VISIBLE);
        binding.rvBrands.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.progressLayout.setVisibility(View.GONE);
        binding.rvBrands.setVisibility(View.VISIBLE);
        binding.errorLayout.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.progressLayout.setVisibility(View.GONE);
        binding.rvBrands.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.VISIBLE);
        binding.tvError.setText(message);
    }
}
