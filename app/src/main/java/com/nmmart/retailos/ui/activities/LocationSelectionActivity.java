package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class LocationSelectionActivity extends AppCompatActivity {

    private Spinner spinnerCity, spinnerArea;
    private MaterialButton btnSubmit;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_selection);

        sessionManager = new SessionManager(this);

        spinnerCity = findViewById(R.id.spinnerCity);
        spinnerArea = findViewById(R.id.spinnerArea);
        btnSubmit = findViewById(R.id.btnSubmitLocation);

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