package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

public class DebugAuditActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_audit);

        sessionManager = new SessionManager(this);

        setupButtons();
    }

    private void setupButtons() {
        findViewById(R.id.btnSplash).setOnClickListener(v -> launchActivity(SplashActivity.class));
        findViewById(R.id.btnOnboarding).setOnClickListener(v -> launchActivity(OnboardingActivity.class));
        findViewById(R.id.btnLogin).setOnClickListener(v -> launchActivity(LoginActivity.class));
        findViewById(R.id.btnSignup).setOnClickListener(v -> launchActivity(SignupActivity.class));
        findViewById(R.id.btnMain).setOnClickListener(v -> launchActivity(MainActivity.class));
        findViewById(R.id.btnLocation).setOnClickListener(v -> launchActivity(LocationSelectionActivity.class));
        
        findViewById(R.id.btnProductList).setOnClickListener(v -> launchActivity(ProductListActivity.class));
        findViewById(R.id.btnProductDetail).setOnClickListener(v -> launchActivity(ProductDetailActivity.class));
        findViewById(R.id.btnSubCategory).setOnClickListener(v -> launchActivity(SubCategoryActivity.class));
        findViewById(R.id.btnCart).setOnClickListener(v -> launchActivity(CartActivity.class));
        findViewById(R.id.btnCheckout).setOnClickListener(v -> launchActivity(CheckoutActivity.class));
        findViewById(R.id.btnWishlist).setOnClickListener(v -> launchActivity(WishlistActivity.class));
        
        findViewById(R.id.btnProfile).setOnClickListener(v -> launchActivity(ProfileActivity.class));
        findViewById(R.id.btnAddress).setOnClickListener(v -> launchActivity(AddressActivity.class));
        findViewById(R.id.btnAddressList).setOnClickListener(v -> launchActivity(AddressListActivity.class));
        findViewById(R.id.btnWallet).setOnClickListener(v -> launchActivity(WalletActivity.class));
        findViewById(R.id.btnSettings).setOnClickListener(v -> launchActivity(SettingsActivity.class));
        
        findViewById(R.id.btnOrderHistory).setOnClickListener(v -> launchActivity(OrderHistoryActivity.class));
        findViewById(R.id.btnOrderTracking).setOnClickListener(v -> launchActivity(OrderTrackingActivity.class));
        findViewById(R.id.btnOrderSuccess).setOnClickListener(v -> launchActivity(OrderSuccessActivity.class));
        findViewById(R.id.btnCustomerSupport).setOnClickListener(v -> launchActivity(CustomerSupportActivity.class));
        findViewById(R.id.btnAboutUs).setOnClickListener(v -> launchActivity(AboutUsActivity.class));
        findViewById(R.id.btnReferEarn).setOnClickListener(v -> launchActivity(ReferEarnActivity.class));
        
        findViewById(R.id.btnClearSession).setOnClickListener(v -> clearSession());
        findViewById(R.id.btnRestartApp).setOnClickListener(v -> restartApp());
    }

    private void launchActivity(Class<?> activityClass) {
        try {
            Intent intent = new Intent(this, activityClass);
            startActivity(intent);
            Toast.makeText(this, "Launching: " + activityClass.getSimpleName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void clearSession() {
        sessionManager.logout();
        Toast.makeText(this, "Session cleared & logged out!", Toast.LENGTH_SHORT).show();
    }

    private void restartApp() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}
