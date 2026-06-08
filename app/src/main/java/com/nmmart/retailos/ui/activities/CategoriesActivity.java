package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Category;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CategoriesActivity extends AppCompatActivity {

    private RecyclerView rvCategories;
    private CategoryAdapter adapter;
    private List<Category> categoryList = new ArrayList<>();
    private View loadingView;
    private View errorView;
    private SupabaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_category);

        repository = new SupabaseRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Categories");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvCategories = findViewById(R.id.rvSubCategories);
        rvCategories.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new CategoryAdapter(categoryList);
        rvCategories.setAdapter(adapter);

        loadingView = findViewById(R.id.shimmerLayout);
        errorView = findViewById(R.id.errorLayout);

        loadAllCategories();
    }

    private void loadAllCategories() {
        showLoading();
        repository.getCategories(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryList.clear();
                    categoryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                    showContent();
                } else {
                    showError("Failed to load categories");
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showLoading() {
        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
            rvCategories.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        rvCategories.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        if (loadingView != null) loadingView.setVisibility(View.GONE);
        rvCategories.setVisibility(View.GONE);
        if (errorView != null) {
            errorView.setVisibility(View.VISIBLE);
            TextView errorText = errorView.findViewById(R.id.tvError);
            if (errorText != null) {
                errorText.setText(message);
            }
            View retryBtn = errorView.findViewById(R.id.btnRetry);
            if (retryBtn != null) {
                retryBtn.setOnClickListener(v -> loadAllCategories());
            }
        }
    }

    private class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {
        private List<Category> items;

        CategoryAdapter(List<Category> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_category, parent, false);
            return new CategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
            Category category = items.get(position);
            holder.tvName.setText(category.getName());

            String imageUrl = category.getImageUrl() != null && !category.getImageUrl().isEmpty() 
                    ? category.getImageUrl() 
                    : category.getIconUrl();

            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_grocery_bag)
                        .error(R.drawable.ic_grocery_bag)
                        .into(holder.ivImage);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(CategoriesActivity.this, SubCategoryActivity.class);
                intent.putExtra("CATEGORY_NAME", category.getName());
                intent.putExtra("CATEGORY_ID", category.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class CategoryViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName;

            CategoryViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivSubCategory);
                tvName = itemView.findViewById(R.id.tvSubCategoryName);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
