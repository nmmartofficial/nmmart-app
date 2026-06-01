package com.nmmart.retailos.ui.activities;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SupabaseRepository;
import com.nmmart.retailos.models.PincodeMaster;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.nmmart.retailos.data.CartManager;
import com.nmmart.retailos.data.SessionManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddressActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SupabaseRepository repository;
    private List<PincodeMaster> pincodes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address);

        sessionManager = new SessionManager(this);
        repository = new SupabaseRepository();

        MaterialButton btnConfirm = findViewById(R.id.btnConfirmAddress);
        TextInputEditText etName = findViewById(R.id.etAddressName);
        TextInputEditText etHouse = findViewById(R.id.etHouse);
        TextInputEditText etPin = findViewById(R.id.etPinCode);

        // Fetch pincodes from Supabase
        fetchPincodes();

        btnConfirm.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String house = etHouse.getText().toString().trim();
            String pin = etPin.getText().toString().trim();
            
            if (name.isEmpty() || house.isEmpty() || pin.isEmpty()) {
                Toast.makeText(this, "Bhai, pata toh sahi se bharo!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Pin Code Blocker Logic using real data
            if (pincodes != null && !pincodes.isEmpty()) {
                boolean isAllowed = false;
                for (PincodeMaster p : pincodes) {
                    if (p.pincode.equals(pin) && p.isAllowed) {
                        isAllowed = true;
                        break;
                    }
                }
                if (!isAllowed) {
                    Toast.makeText(this, "NM Mart doesn't deliver to this area yet!", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                // Fallback if no pincodes loaded (keep old logic for safety)
                if (!pin.equals("212201")) {
                    Toast.makeText(this, "NM Mart doesn't deliver to this area yet!", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            // Save Address to Supabase
            saveAddressToDatabase(name, house, pin);
        });
    }

    private void saveAddressToDatabase(String name, String house, String pin) {
        Toast.makeText(this, "Saving address...", Toast.LENGTH_SHORT).show();
        
        Map<String, Object> addressData = new HashMap<>();
        addressData.put("user_id", sessionManager.getUserId());
        addressData.put("full_name", name);
        addressData.put("house_no", house);
        addressData.put("pincode", pin);
        addressData.put("city", "Manjhanpur"); // Default for now
        addressData.put("is_default", true);

        repository.addAddress(addressData, new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddressActivity.this, "Address saved successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to Checkout
                } else {
                    Toast.makeText(AddressActivity.this, "Failed to save address", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(AddressActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
