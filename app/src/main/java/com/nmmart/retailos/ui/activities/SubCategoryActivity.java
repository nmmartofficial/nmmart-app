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

public class SubCategoryActivity extends AppCompatActivity {

    private RecyclerView rvSubCategories;
    private SubCategoryAdapter adapter;
    private List<Category> subCategoryList = new ArrayList<>();
    private String parentCategoryName;
    private String parentCategoryId;
    private SupabaseRepository supabaseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_category);

        parentCategoryName = getIntent().getStringExtra("CATEGORY_NAME");
        parentCategoryId = getIntent().getStringExtra("CATEGORY_ID");
        supabaseRepository = new SupabaseRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(parentCategoryName);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvSubCategories = findViewById(R.id.rvSubCategories);
        rvSubCategories.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new SubCategoryAdapter(subCategoryList);
        rvSubCategories.setAdapter(adapter);

        loadSubCategories();
    }

    private void loadSubCategories() {
        supabaseRepository.getSubCategories(parentCategoryId, new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    subCategoryList.clear();
                    subCategoryList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    // If no subcategories found, let's open ProductListActivity directly
                    Intent intent = new Intent(SubCategoryActivity.this, ProductListActivity.class);
                    intent.putExtra("CATEGORY_NAME", parentCategoryName);
                    startActivity(intent);
                    finish();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                // On failure, open ProductListActivity directly
                Intent intent = new Intent(SubCategoryActivity.this, ProductListActivity.class);
                intent.putExtra("CATEGORY_NAME", parentCategoryName);
                startActivity(intent);
                finish();
            }
        });
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
            
            if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
                Glide.with(SubCategoryActivity.this)
                        .load(category.getImageUrl())
                        .placeholder(R.drawable.ic_grocery_bag)
                        .into(holder.ivImage);
            }
            
            holder.itemView.setOnClickListener(v -> {
                // Check if this subcategory has further subcategories
                // For now, let's open ProductListActivity
                Intent intent = new Intent(SubCategoryActivity.this, ProductListActivity.class);
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
