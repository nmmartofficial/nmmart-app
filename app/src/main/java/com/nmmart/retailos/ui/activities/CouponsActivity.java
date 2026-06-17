package com.nmmart.retailos.ui.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.Coupon;
import com.nmmart.retailos.ui.adapters.CouponAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CouponsActivity extends AppCompatActivity {

    private RecyclerView rvCoupons;
    private LinearLayout layoutEmptyState;
    private CouponAdapter adapter;
    private SupabaseRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coupons);

        repository = new SupabaseRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvCoupons = findViewById(R.id.rvCoupons);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        setupRecyclerView();
        fetchCoupons();
    }

    private void setupRecyclerView() {
        adapter = new CouponAdapter(this, new ArrayList<>());
        rvCoupons.setLayoutManager(new LinearLayoutManager(this));
        rvCoupons.setAdapter(adapter);
    }

    private void fetchCoupons() {
        repository.getCoupons(new Callback<List<Coupon>>() {
            @Override
            public void onResponse(Call<List<Coupon>> call, Response<List<Coupon>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    layoutEmptyState.setVisibility(View.GONE);
                    rvCoupons.setVisibility(View.VISIBLE);
                    adapter.updateCoupons(response.body());
                } else {
                    rvCoupons.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<List<Coupon>> call, Throwable t) {
                Toast.makeText(CouponsActivity.this, "Failed to load coupons", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
