package com.nmmart.retailos.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.nmmart.retailos.R;
import com.nmmart.retailos.data.SessionManager;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TabLayout tabLayout;
    private Button btnNext, btnGetStarted;
    private SessionManager sessionManager;

    private int[] onboardingImages = {
        R.drawable.ic_grocery_bag,
        R.drawable.ic_wallet,
        R.drawable.ic_person
    };

    private String[] onboardingTitles = {
        "Welcome to NM Mart!",
        "Fast & Easy Checkout",
        "Great Offers & Savings"
    };

    private String[] onboardingDescriptions = {
        "Your local grocery store at your fingertips!",
        "Multiple payment options and fast delivery!",
        "Earn rewards and save on every order!"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        sessionManager = new SessionManager(this);
        
        // Check if user has already seen onboarding
        if (sessionManager.isOnboardingCompleted()) {
            navigateToNextScreen();
            finish();
            return;
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btnNext);
        btnGetStarted = findViewById(R.id.btnGetStarted);

        OnboardingPagerAdapter adapter = new OnboardingPagerAdapter();
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                if (position == onboardingTitles.length - 1) {
                    btnNext.setVisibility(View.GONE);
                    btnGetStarted.setVisibility(View.VISIBLE);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                    btnGetStarted.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < onboardingTitles.length - 1) {
                viewPager.setCurrentItem(current + 1);
            }
        });

        btnGetStarted.setOnClickListener(v -> {
            sessionManager.setOnboardingCompleted(true);
            navigateToNextScreen();
        });
    }

    private void navigateToNextScreen() {
        if (sessionManager.isLoggedIn()) {
            String location = sessionManager.getDeliveryLocation();
            if (location == null || location.isEmpty() || location.equals("Select Location")) {
                startActivity(new Intent(this, LocationSelectionActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        } else {
            startActivity(new Intent(this, LoginActivity.class));
        }
        finish();
    }

    private class OnboardingPagerAdapter extends PagerAdapter {

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            LayoutInflater inflater = getLayoutInflater();
            View view = inflater.inflate(R.layout.item_onboarding, container, false);

            ImageView imageView = view.findViewById(R.id.ivOnboarding);
            TextView tvTitle = view.findViewById(R.id.tvOnboardingTitle);
            TextView tvDescription = view.findViewById(R.id.tvOnboardingDescription);

            imageView.setImageResource(onboardingImages[position]);
            tvTitle.setText(onboardingTitles[position]);
            tvDescription.setText(onboardingDescriptions[position]);

            container.addView(view);
            return view;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }

        @Override
        public int getCount() {
            return onboardingTitles.length;
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }
    }
}
