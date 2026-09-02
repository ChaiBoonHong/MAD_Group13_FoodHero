package com.uccd3223.group13.mad_group13_foodhero.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.uccd3223.group13.mad_group13_foodhero.R;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.DataError;
import com.uccd3223.group13.mad_group13_foodhero.data.callback.ResultCallback;
import com.uccd3223.group13.mad_group13_foodhero.data.model.Profile;
import com.uccd3223.group13.mad_group13_foodhero.data.model.UserRole;
import com.uccd3223.group13.mad_group13_foodhero.data.repository.AuthRepository;

public class SplashActivity extends AppCompatActivity {
    private AuthRepository authRepo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        authRepo = AuthRepository.getInstance(this);

        // Delay 1.2s to show splash branding and check stored session
        new Handler(Looper.getMainLooper()).postDelayed(this::checkSessionAndRoute, 1200);
    }

    private void checkSessionAndRoute() {
        if (!authRepo.isLoggedIn()) {
            navigateToAuth();
            return;
        }

        authRepo.restoreSession(new ResultCallback<Profile>() {
            @Override
            public void onSuccess(Profile profile) {
                if (profile != null && profile.getRole() == UserRole.MERCHANT) {
                    navigateToMerchantHome();
                } else {
                    navigateToStudentHome();
                }
            }

            @Override
            public void onError(DataError error) {
                // If offline but profile exists, route based on cached role
                Profile cached = authRepo.getCurrentProfile();
                if (cached != null) {
                    if (cached.getRole() == UserRole.MERCHANT) {
                        navigateToMerchantHome();
                    } else {
                        navigateToStudentHome();
                    }
                } else {
                    navigateToAuth();
                }
            }
        });
    }

    private void navigateToAuth() {
        startActivity(new Intent(this, AuthActivity.class));
        finish();
    }

    private void navigateToStudentHome() {
        startActivity(new Intent(this, StudentHomeActivity.class));
        finish();
    }

    private void navigateToMerchantHome() {
        startActivity(new Intent(this, MerchantHomeActivity.class));
        finish();
    }
}
