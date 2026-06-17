package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.nmmart.retailos.R;
import com.nmmart.retailos.models.Address;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.data.SessionManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressListActivity extends AppCompatActivity {

    private RecyclerView rvAddresses;
    private AddressAdapter adapter;
    private List<Address> addressList = new ArrayList<>();
    private SupabaseRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);

        sessionManager = new SessionManager(this);
        repository = new SupabaseRepository();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvAddresses = findViewById(R.id.rvAddresses);
        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter(addressList);
        rvAddresses.setAdapter(adapter);

        FloatingActionButton fabAdd = findViewById(R.id.fabAddAddress);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddressActivity.class);
            startActivity(intent);
        });

        loadAddresses();
    }

    private void loadAddresses() {
        if (!sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Please login to view addresses", Toast.LENGTH_SHORT).show();
            return;
        }
        
        repository.getUserAddresses(sessionManager.getUserId(), new Callback<List<Address>>() {
            @Override
            public void onResponse(Call<List<Address>> call, Response<List<Address>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    addressList.clear();
                    addressList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Address>> call, Throwable t) {
                Toast.makeText(AddressListActivity.this, "Failed to load addresses", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {
        private List<Address> addresses;

        AddressAdapter(List<Address> addresses) {
            this.addresses = addresses;
        }

        @NonNull
        @Override
        public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
            return new AddressViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
            Address address = addresses.get(position);
            holder.tvName.setText(address.fullName);
            holder.tvAddress.setText(address.houseNo + ", " + address.city + " - " + address.pincode);
            holder.ivDefault.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);

            holder.itemView.setOnClickListener(v -> {
                // Select this address as default
                Toast.makeText(AddressListActivity.this, "Address selected!", Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        @Override
        public int getItemCount() {
            return addresses.size();
        }

        class AddressViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvAddress;
            ImageView ivDefault, ivEdit, ivDelete;

            AddressViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvAddressName);
                tvAddress = itemView.findViewById(R.id.tvAddressDetails);
                ivDefault = itemView.findViewById(R.id.ivDefault);
                ivEdit = itemView.findViewById(R.id.ivEdit);
                ivDelete = itemView.findViewById(R.id.ivDelete);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh address list when coming back
        loadAddresses();
    }
}
