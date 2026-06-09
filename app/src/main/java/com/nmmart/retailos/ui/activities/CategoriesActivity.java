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
import androidx.recyclerview.widget.LinearLayoutManager;
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

    private RecyclerView rvMainCategories;
    private RecyclerView rvSubCategories;
    private MainCategoryAdapter mainCategoryAdapter;
    private SubCategoryAdapter subCategoryAdapter;
    private List<Category> mainCategoryList = new ArrayList<>();
    private List<Category> subCategoryList = new ArrayList<>();
    private View emptyState;
    private View loadingView;
    private View errorView;
    private TextView tvError;
    private View btnRetry;
    private SupabaseRepository repository;
    private Category selectedMainCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_categories);

            repository = new SupabaseRepository();

            MaterialToolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setTitle("Categories");
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            }

            // Initialize views with null checks
            rvMainCategories = findViewById(R.id.rvMainCategories);
            rvSubCategories = findViewById(R.id.rvSubCategories);
            emptyState = findViewById(R.id.emptyState);
            loadingView = findViewById(R.id.shimmerLayout);
            errorView = findViewById(R.id.errorLayout);
            tvError = findViewById(R.id.tvError);
            btnRetry = findViewById(R.id.btnRetry);

            // Setup main categories (left panel)
            if (rvMainCategories != null) {
                rvMainCategories.setLayoutManager(new LinearLayoutManager(this));
                mainCategoryAdapter = new MainCategoryAdapter(mainCategoryList);
                rvMainCategories.setAdapter(mainCategoryAdapter);
            }

            // Setup subcategories (right panel)
            if (rvSubCategories != null) {
                rvSubCategories.setLayoutManager(new GridLayoutManager(this, 2));
                subCategoryAdapter = new SubCategoryAdapter(subCategoryList);
                rvSubCategories.setAdapter(subCategoryAdapter);
            }

            if (btnRetry != null) {
                btnRetry.setOnClickListener(v -> {
                    if (selectedMainCategory != null) {
                        loadSubCategories(selectedMainCategory);
                    } else {
                        loadMainCategories();
                    }
                });
            }

            loadMainCategories();
        } catch (Exception e) {
            e.printStackTrace();
            finish(); // Close activity if something goes wrong
        }
    }

    private void loadMainCategories() {
        try {
            final String selectedCategoryId = getIntent().getStringExtra("selected_category_id");
            showMainLoading();
            repository.getCategories(new Callback<List<Category>>() {
                @Override
                public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            mainCategoryList.clear();
                            // Filter main categories (parent_id is null or empty)
                            for (Category cat : response.body()) {
                                if (cat.getParentId() == null || cat.getParentId().isEmpty()) {
                                    mainCategoryList.add(cat);
                                }
                            }
                            mainCategoryAdapter.notifyDataSetChanged();
                            showMainContent();
                            
                            // Auto-select the category from intent if available
                            if (selectedCategoryId != null && !selectedCategoryId.isEmpty()) {
                                for (int i = 0; i < mainCategoryList.size(); i++) {
                                    Category cat = mainCategoryList.get(i);
                                    if (cat.getId() != null && cat.getId().equals(selectedCategoryId)) {
                                        mainCategoryAdapter.setSelectedPosition(i);
                                        loadSubCategories(cat);
                                        break;
                                    }
                                }
                            }
                        } else {
                            showMainError("Failed to load categories");
                        }
                    } catch (Exception e) {
                        showMainError("Error loading categories: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(Call<List<Category>> call, Throwable t) {
                    showMainError("Network error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            showMainError("Error: " + e.getMessage());
        }
    }

    private void loadSubCategories(Category mainCategory) {
        try {
            if (mainCategory == null || mainCategory.getId() == null) {
                return;
            }
            this.selectedMainCategory = mainCategory;
            showRightLoading();
            repository.getSubCategories(mainCategory.getId(), new Callback<List<Category>>() {
                @Override
                public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            subCategoryList.clear();
                            List<Category> subCats = response.body();
                            if (subCats.isEmpty()) {
                                // No subcategories, show products directly
                                Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                                intent.putExtra("CATEGORY_NAME", mainCategory.getName());
                                intent.putExtra("CATEGORY_ID", mainCategory.getId());
                                startActivity(intent);
                            } else {
                                subCategoryList.addAll(subCats);
                                subCategoryAdapter.notifyDataSetChanged();
                                showRightSubCategories();
                            }
                        } else {
                            // If error, treat as no subcategories and show products directly
                            Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                            intent.putExtra("CATEGORY_NAME", mainCategory.getName());
                            intent.putExtra("CATEGORY_ID", mainCategory.getId());
                            startActivity(intent);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        // Fallback to product list
                        Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                        intent.putExtra("CATEGORY_NAME", mainCategory.getName());
                        intent.putExtra("CATEGORY_ID", mainCategory.getId());
                        startActivity(intent);
                    }
                }

                @Override
                public void onFailure(Call<List<Category>> call, Throwable t) {
                    try {
                        // On failure, show products directly
                        Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                        intent.putExtra("CATEGORY_NAME", mainCategory.getName());
                        intent.putExtra("CATEGORY_ID", mainCategory.getId());
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback to product list
            try {
                Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                intent.putExtra("CATEGORY_NAME", mainCategory.getName());
                intent.putExtra("CATEGORY_ID", mainCategory.getId());
                startActivity(intent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // Main panel state methods
    private void showMainLoading() {
        try {
            if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
            if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (rvSubCategories != null) rvSubCategories.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMainContent() {
        try {
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (rvMainCategories != null) rvMainCategories.setVisibility(View.VISIBLE);
            if (errorView != null) errorView.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showMainError(String message) {
        try {
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (rvMainCategories != null) rvMainCategories.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (tvError != null) tvError.setText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Right panel state methods
    private void showRightLoading() {
        try {
            if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (rvSubCategories != null) rvSubCategories.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRightSubCategories() {
        try {
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (rvSubCategories != null) rvSubCategories.setVisibility(View.VISIBLE);
            if (errorView != null) errorView.setVisibility(View.GONE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showRightError(String message) {
        try {
            if (loadingView != null) loadingView.setVisibility(View.GONE);
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            if (rvSubCategories != null) rvSubCategories.setVisibility(View.GONE);
            if (errorView != null) errorView.setVisibility(View.VISIBLE);
            if (tvError != null) tvError.setText(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class MainCategoryAdapter extends RecyclerView.Adapter<MainCategoryAdapter.MainCategoryViewHolder> {
        private List<Category> items;
        private int selectedPosition = -1;

        MainCategoryAdapter(List<Category> items) {
            this.items = items;
        }
        
        public void setSelectedPosition(int position) {
            int previous = selectedPosition;
            selectedPosition = position;
            if (previous >= 0) notifyItemChanged(previous);
            if (selectedPosition >= 0) notifyItemChanged(selectedPosition);
        }

        @NonNull
        @Override
        public MainCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_main_category, parent, false);
            return new MainCategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MainCategoryViewHolder holder, int position) {
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
                        .into(holder.ivCategory);
            }

            // Highlight selected category
            if (position == selectedPosition) {
                holder.itemView.setBackgroundResource(android.R.color.white);
                holder.tvName.setTextColor(getResources().getColor(R.color.orange_primary));
            } else {
                holder.itemView.setBackgroundResource(android.R.color.transparent);
                holder.tvName.setTextColor(getResources().getColor(R.color.black_soft));
            }

            holder.itemView.setOnClickListener(v -> {
                int previous = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
                loadSubCategories(category);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class MainCategoryViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCategory;
            TextView tvName;

            MainCategoryViewHolder(View itemView) {
                super(itemView);
                ivCategory = itemView.findViewById(R.id.ivCategory);
                tvName = itemView.findViewById(R.id.tvName);
            }
        }
    }

    private class SubCategoryAdapter extends RecyclerView.Adapter<SubCategoryAdapter.SubCategoryViewHolder> {
        private List<Category> items;

        SubCategoryAdapter(List<Category> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public SubCategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_category, parent, false);
            return new SubCategoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SubCategoryViewHolder holder, int position) {
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
                Intent intent = new Intent(CategoriesActivity.this, ProductListActivity.class);
                intent.putExtra("CATEGORY_NAME", category.getName());
                intent.putExtra("CATEGORY_ID", category.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class SubCategoryViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvName;

            SubCategoryViewHolder(View itemView) {
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
