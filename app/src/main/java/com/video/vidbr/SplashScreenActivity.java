package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class SplashScreenActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_LENGTH = 3000; // 3 seconds
    private WeakReference<SplashScreenActivity> weakActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        weakActivity = new WeakReference<>(this); // Initialize the weak reference

        // Navigate to MainActivity after 3 seconds
        new Handler().postDelayed(this::navigateToMainActivity, SPLASH_DISPLAY_LENGTH);
    }

    private void navigateToMainActivity() {
        Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
        startActivity(mainIntent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Clear weak reference to avoid memory leaks if necessary
        if (weakActivity != null) {
            weakActivity.clear();
        }
    }
}
