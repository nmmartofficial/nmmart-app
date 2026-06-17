package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.databinding.ActivityOrderHistoryBinding;
import com.nmmart.retailos.databinding.ItemOrderBinding;
import com.nmmart.retailos.models.Order;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderHistoryActivity extends BaseActivity {

    private ActivityOrderHistoryBinding binding;
    private SupabaseRepository repository;
    private List<Order> originalOrdersList = new ArrayList<>();
    private List<Order> filteredOrdersList = new ArrayList<>();
    private OrderAdapter adapter;
    
    private String currentStatusFilter = "all";
    private String currentSortBy = "date_desc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        repository = new SupabaseRepository();
        setupToolbar();
        setupRecyclerView();
        loadOrders();

        binding.btnStartShopping.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.my_orders);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_order_history, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            showFilterSortBottomSheet();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showFilterSortBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_order_filter, null);
        bottomSheetDialog.setContentView(view);
        
        RadioGroup rgStatusFilter = view.findViewById(R.id.rgStatusFilter);
        RadioGroup rgSortBy = view.findViewById(R.id.rgSortBy);
        
        // Pre-select current options
        if ("pending".equals(currentStatusFilter)) {
            rgStatusFilter.check(R.id.rbPending);
        } else if ("shipped".equals(currentStatusFilter)) {
            rgStatusFilter.check(R.id.rbShipped);
        } else if ("delivered".equals(currentStatusFilter)) {
            rgStatusFilter.check(R.id.rbDelivered);
        } else if ("cancelled".equals(currentStatusFilter)) {
            rgStatusFilter.check(R.id.rbCancelled);
        } else {
            rgStatusFilter.check(R.id.rbAll);
        }
        
        if ("date_desc".equals(currentSortBy)) {
            rgSortBy.check(R.id.rbDateDesc);
        } else if ("date_asc".equals(currentSortBy)) {
            rgSortBy.check(R.id.rbDateAsc);
        } else if ("total_desc".equals(currentSortBy)) {
            rgSortBy.check(R.id.rbTotalDesc);
        } else if ("total_asc".equals(currentSortBy)) {
            rgSortBy.check(R.id.rbTotalAsc);
        }
        
        view.findViewById(R.id.btnApply).setOnClickListener(v -> {
            int selectedStatusId = rgStatusFilter.getCheckedRadioButtonId();
            if (selectedStatusId == R.id.rbPending) {
                currentStatusFilter = "pending";
            } else if (selectedStatusId == R.id.rbShipped) {
                currentStatusFilter = "shipped";
            } else if (selectedStatusId == R.id.rbDelivered) {
                currentStatusFilter = "delivered";
            } else if (selectedStatusId == R.id.rbCancelled) {
                currentStatusFilter = "cancelled";
            } else {
                currentStatusFilter = "all";
            }
            
            int selectedSortId = rgSortBy.getCheckedRadioButtonId();
            if (selectedSortId == R.id.rbDateAsc) {
                currentSortBy = "date_asc";
            } else if (selectedSortId == R.id.rbTotalDesc) {
                currentSortBy = "total_desc";
            } else if (selectedSortId == R.id.rbTotalAsc) {
                currentSortBy = "total_asc";
            } else {
                currentSortBy = "date_desc";
            }
            
            applyFilterAndSort();
            bottomSheetDialog.dismiss();
        });
        
        view.findViewById(R.id.btnReset).setOnClickListener(v -> {
            currentStatusFilter = "all";
            currentSortBy = "date_desc";
            applyFilterAndSort();
            bottomSheetDialog.dismiss();
        });
        
        bottomSheetDialog.show();
    }
    
    private void applyFilterAndSort() {
        filteredOrdersList.clear();
        
        // Filter
        for (Order order : originalOrdersList) {
            String orderStatus = order.orderStatus != null ? order.orderStatus : order.status;
            orderStatus = orderStatus != null ? orderStatus.toLowerCase() : "pending";
            
            if (currentStatusFilter.equals("all") || currentStatusFilter.equals(orderStatus)) {
                filteredOrdersList.add(order);
            }
        }
        
        // Sort
        if (currentSortBy.equals("date_desc")) {
            Collections.sort(filteredOrdersList, (o1, o2) -> {
                String date1 = o1.createdAt != null ? o1.createdAt : "";
                String date2 = o2.createdAt != null ? o2.createdAt : "";
                return date2.compareTo(date1);
            });
        } else if (currentSortBy.equals("date_asc")) {
            Collections.sort(filteredOrdersList, (o1, o2) -> {
                String date1 = o1.createdAt != null ? o1.createdAt : "";
                String date2 = o2.createdAt != null ? o2.createdAt : "";
                return date1.compareTo(date2);
            });
        } else if (currentSortBy.equals("total_desc")) {
            Collections.sort(filteredOrdersList, (o1, o2) -> {
                double total1 = o1.totalAmount != null ? o1.totalAmount : 0.0;
                double total2 = o2.totalAmount != null ? o2.totalAmount : 0.0;
                return Double.compare(total2, total1);
            });
        } else if (currentSortBy.equals("total_asc")) {
            Collections.sort(filteredOrdersList, (o1, o2) -> {
                double total1 = o1.totalAmount != null ? o1.totalAmount : 0.0;
                double total2 = o2.totalAmount != null ? o2.totalAmount : 0.0;
                return Double.compare(total1, total2);
            });
        }
        
        adapter.notifyDataSetChanged();
        
        if (filteredOrdersList.isEmpty()) {
            showEmptyState();
        } else {
            showOrdersList();
        }
    }

    private void setupRecyclerView() {
        adapter = new OrderAdapter(filteredOrdersList);
        binding.rvOrders.setLayoutManager(new LinearLayoutManager(this));
        binding.rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        if (!sessionManager.isLoggedIn()) {
            showEmptyState();
            return;
        }

        repository.getUserOrders(sessionManager.getUserId(), new Callback<List<Order>>() {
            @Override
            public void onResponse(Call<List<Order>> call, Response<List<Order>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    originalOrdersList.clear();
                    originalOrdersList.addAll(response.body());
                    applyFilterAndSort();
                } else {
                    Toast.makeText(OrderHistoryActivity.this, "Failed to load orders", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                }
            }

            @Override
            public void onFailure(Call<List<Order>> call, Throwable t) {
                Toast.makeText(OrderHistoryActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        binding.rvOrders.setVisibility(View.GONE);
        binding.emptyOrdersLayout.setVisibility(View.VISIBLE);
    }

    private void showOrdersList() {
        binding.rvOrders.setVisibility(View.VISIBLE);
        binding.emptyOrdersLayout.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
        private List<Order> items;

        public OrderAdapter(List<Order> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemOrderBinding itemBinding = ItemOrderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order order = items.get(position);
            holder.binding.tvOrderId.setText("Order #" + (order.id.length() > 8 ? order.id.substring(0, 8) : order.id));
            holder.binding.tvOrderTotal.setText("₹" + String.format("%.0f", order.totalAmount != null ? order.totalAmount : 0.0));
            holder.binding.tvOrderItems.setText(order.itemsSummary != null ? order.itemsSummary : "Order Items");
            
            // Use orderStatus if available, else status
            String orderStatusValue = order.orderStatus != null ? order.orderStatus : order.status;
            holder.binding.tvStatus.setText(orderStatusValue != null ? orderStatusValue.toUpperCase() : "PENDING");

            // Format date if needed (assuming ISO format from Supabase)
            if (order.createdAt != null && order.createdAt.length() >= 10) {
                holder.binding.tvOrderDate.setText(order.createdAt.substring(0, 10));
            }

            // Click to open order tracking
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(OrderHistoryActivity.this, OrderTrackingActivity.class);
                intent.putExtra("order_id", order.id);
                intent.putExtra("order_status", orderStatusValue);
                intent.putExtra("expected_delivery", order.expectedDelivery != null ? order.expectedDelivery : "Soon");
                startActivity(intent);
            });

            // Timeline logic
            String status = orderStatusValue != null ? orderStatusValue.toLowerCase() : "pending";
            resetTimeline(holder);
            
            if (status.equals("pending")) {
                holder.binding.dotPending.setBackgroundColor(getResources().getColor(R.color.primary_dark));
                holder.binding.btnCancelOrder.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAgain.setVisibility(View.GONE);
                holder.binding.btnCancelOrder.setOnClickListener(v -> {
                    new AlertDialog.Builder(OrderHistoryActivity.this)
                        .setTitle("Cancel Order")
                        .setMessage("Are you sure you want to cancel this order?")
                        .setPositiveButton("Yes, Cancel", (dialog, which) -> {
                            cancelOrder(order.id);
                        })
                        .setNegativeButton("No", null)
                        .show();
                });
            } else if (status.equals("shipped")) {
                holder.binding.dotPending.setBackgroundColor(getResources().getColor(R.color.primary_dark));
                holder.binding.dotShipped.setBackgroundColor(getResources().getColor(R.color.primary_dark));
                holder.binding.btnCancelOrder.setVisibility(View.GONE);
                holder.binding.btnOrderAgain.setVisibility(View.GONE);
            } else if (status.equals("delivered")) {
                holder.binding.dotPending.setBackgroundColor(getResources().getColor(R.color.green_success));
                holder.binding.dotShipped.setBackgroundColor(getResources().getColor(R.color.green_success));
                holder.binding.dotDelivered.setBackgroundColor(getResources().getColor(R.color.green_success));
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"));
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                holder.binding.btnCancelOrder.setVisibility(View.GONE);
                holder.binding.btnOrderAgain.setVisibility(View.VISIBLE);
                holder.binding.btnReturnOrder.setVisibility(View.VISIBLE);
                holder.binding.btnOrderAgain.setOnClickListener(v -> {
                    Intent intent = new Intent(OrderHistoryActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    Toast.makeText(OrderHistoryActivity.this, "Go to home to reorder items!", Toast.LENGTH_SHORT).show();
                });
                holder.binding.btnReturnOrder.setOnClickListener(v -> {
                    new AlertDialog.Builder(OrderHistoryActivity.this)
                        .setTitle("Return Order")
                        .setMessage("Are you sure you want to return this order?")
                        .setPositiveButton("Yes, Return", (dialog, which) -> {
                            returnOrder(order.id);
                        })
                        .setNegativeButton("No", null)
                        .show();
                });
            } else if (status.equals("cancelled")) {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"));
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                holder.binding.btnCancelOrder.setVisibility(View.GONE);
                holder.binding.btnOrderAgain.setVisibility(View.GONE);
                holder.binding.btnReturnOrder.setVisibility(View.GONE);
            } else if (status.equals("returned")) {
                holder.binding.cardStatus.setCardBackgroundColor(android.graphics.Color.parseColor("#FFEBEE"));
                holder.binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#D32F2F"));
                holder.binding.btnCancelOrder.setVisibility(View.GONE);
                holder.binding.btnOrderAgain.setVisibility(View.GONE);
                holder.binding.btnReturnOrder.setVisibility(View.GONE);
            }
        }

        private void cancelOrder(String orderId) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "Cancelled");
            repository.updateOrderStatus(orderId, data, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrderHistoryActivity.this, "Order Cancelled Successfully", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }

        private void returnOrder(String orderId) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("status", "Returned");
            repository.updateOrderStatus(orderId, data, new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(OrderHistoryActivity.this, "Order Returned Successfully", Toast.LENGTH_SHORT).show();
                        loadOrders();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }

        private void resetTimeline(ViewHolder holder) {
            int gray = android.graphics.Color.parseColor("#EEEEEE");
            holder.binding.dotPending.setBackgroundColor(gray);
            holder.binding.dotShipped.setBackgroundColor(gray);
            holder.binding.dotDelivered.setBackgroundColor(gray);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ItemOrderBinding binding;

            public ViewHolder(@NonNull ItemOrderBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
