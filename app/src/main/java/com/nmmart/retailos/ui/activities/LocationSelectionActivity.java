package com.nmmart.retailos.ui.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LocationSelectionActivity extends AppCompatActivity {

    private Spinner spinnerCity, spinnerArea;
    private MaterialButton btnSubmit, btnCurrentLocation;
    private SessionManager sessionManager;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selection);

        sessionManager = new SessionManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerArea = findViewById(R.id.spinnerArea);
        btnSubmit = findViewById(R.id.btnSubmitLocation);
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation);

        setupSpinners();

        btnSubmit.setOnClickListener(v -> {
            String city = spinnerCity.getSelectedItem().toString();
            String area = spinnerArea.getSelectedItem().toString();
            
            if (city.equals("Select City") || area.equals("Select Area")) {
                Toast.makeText(this, "Please select city and area", Toast.LENGTH_SHORT).show();
                return;
            }

            sessionManager.setDeliveryLocation(area + ", " + city);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        btnCurrentLocation.setOnClickListener(v -> checkLocationPermission());
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        btnCurrentLocation.setEnabled(false);
        btnCurrentLocation.setText("Fetching location...");

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                fetchAddressFromLocation(location);
            } else {
                btnCurrentLocation.setEnabled(true);
                btnCurrentLocation.setText("Use Current Location");
                Toast.makeText(this, "Could not get location. Please try manually.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAddressFromLocation(Location location) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String area = address.getSubLocality() != null ? address.getSubLocality() : address.getLocality();
                String city = address.getLocality();
                
                if (city != null) {
                    sessionManager.setDeliveryLocation(area + ", " + city);
                    Toast.makeText(this, "Location set: " + area + ", " + city, Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error fetching address", Toast.LENGTH_SHORT).show();
        } finally {
            btnCurrentLocation.setEnabled(true);
            btnCurrentLocation.setText("Use Current Location");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        }
    }

    private void setupSpinners() {
        List<String> cities = new ArrayList<>();
        cities.add("Select City");
        cities.add("Manjhanpur");
        cities.add("Bharwari");

        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, cities);
        cityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(cityAdapter);

        spinnerCity.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAreaSpinner(cities.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateAreaSpinner(String city) {
        List<String> areas = new ArrayList<>();
        areas.add("Select Area");
        
        if (city.equals("Manjhanpur")) {
            areas.add("Naya Nagar");
            areas.add("Main Market");
            areas.add("Civil Lines");
        } else if (city.equals("Bharwari")) {
            areas.add("Railway Station");
            areas.add("Old City");
        }

        ArrayAdapter<String> areaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, areas);
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerArea.setAdapter(areaAdapter);
    }
}