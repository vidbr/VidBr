package com.video.vidbr;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.video.vidbr.databinding.ActivityMainBinding;
import com.video.vidbr.fragments.FollowingFragment;
import com.video.vidbr.fragments.ForYouFragment;
import com.video.vidbr.util.UiUtil;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private boolean isUploading = false;
    private float dX, dY;
    private static final int SHARE_REQUEST_CODE = 1001;

    private WeakReference<Activity> weakActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String language = Locale.getDefault().getLanguage(); // "pt", "en", etc.
        FirebaseMessaging.getInstance().subscribeToTopic("lang_" + language);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                checkAndRequestPermission();
            }
        }

        weakActivity = new WeakReference<>(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Window w = getWindow();
            w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }

        showRateUsDialogIfNeeded();

        setupNavigation();
        setupTabs();

        handleIntent(getIntent());
    }

    private void showRateUsDialogIfNeeded() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);

        int appOpenCount = sharedPreferences.getInt("appOpenCount", 0);

        boolean hasShownRateUsDialog = sharedPreferences.getBoolean("hasShownRateUsDialog", false);

        if ((appOpenCount == 5 || appOpenCount == 10) && !hasShownRateUsDialog) {
            RateUsDialog rateUsDialog = new RateUsDialog(MainActivity.this);
            rateUsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(getResources().getColor(android.R.color.transparent)));
            rateUsDialog.show();

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("hasShownRateUsDialog", true);  // Marca que o diálogo foi mostrado
            editor.apply();
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("appOpenCount", appOpenCount + 1);  // Incrementa o contador
        editor.apply();
    }


    private void checkAndRequestPermission() {
        SharedPreferences sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE);
        boolean hasRequestedPermissionBefore = sharedPreferences.getBoolean("hasRequestedPermissionBefore", false);

        if (!hasRequestedPermissionBefore) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                showPermissionAlertDialog(
                        getString(R.string.permission_alert_message),
                        () -> {
                           requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
                        },
                        () -> {

                        }
                );

                // Atualize o status de solicitação de permissão para garantir que não será solicitado novamente
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("hasRequestedPermissionBefore", true);
                editor.apply();
            }
        }
    }

    private void showPermissionAlertDialog(String message, Runnable onPositiveClick, Runnable onNegativeClick) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button positiveButton = dialogView.findViewById(R.id.dialog_positive_button);
        positiveButton.setOnClickListener(v -> {
            if (onPositiveClick != null) {
                onPositiveClick.run();
            }
            dialog.dismiss();
        });

        Button negativeButton = dialogView.findViewById(R.id.dialog_negative_button);
        negativeButton.setOnClickListener(v -> {
            if (onNegativeClick != null) {
                onNegativeClick.run();
            }
            dialog.dismiss();
        });

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.alert_dialog_background);
        dialog.show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                String videoId = data.getLastPathSegment();
                if (videoId != null) {
                    loadVideo(videoId);
                } else {
                    String profileId = data.getQueryParameter("profile_id");
                    if (profileId != null) {
                        Intent profileIntent = new Intent(MainActivity.this, ProfileActivity.class);
                        profileIntent.putExtra("profile_user_id", profileId);
                        startActivity(profileIntent);
                    }
                }
            }
        }
    }

    private void loadVideo(String videoId) {
        Fragment fragment = ForYouFragment.newInstance(videoId);
        loadFragment(fragment);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SHARE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                SharedPreferences sharedPreferences = getSharedPreferences("VideoPrefs", MODE_PRIVATE);
                String videoId = sharedPreferences.getString("last_shared_video_id", null);
                if (videoId != null) {
                    updateShareCount(videoId);
                }
            }
        }
    }

    private void updateShareCount(String videoId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference videoRef = db.collection("videos").document(videoId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(videoRef);
            long currentShareCount = snapshot.getLong("shareCount") != null ? snapshot.getLong("shareCount") : 0;
            transaction.update(videoRef, "shareCount", currentShareCount + 1);
            return null;
        });
    }

    private void setupNavigation() {
        binding.searchIcon.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SearchActivity.class));
            overridePendingTransition(0, 0);
            finish();
        });

        binding.bottomNavBar.setItemIconTintList(null);

        Menu menu = binding.bottomNavBar.getMenu();
        MenuItem homeItem = menu.findItem(R.id.bottom_menu_home);
        homeItem.setIcon(R.drawable.icon_home);

        binding.bottomNavBar.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.bottom_menu_home) {
                UiUtil.showToast(MainActivity.this, "Home");
            }
            if (itemId == R.id.bottom_menu_add_video) {
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading_video_message), Snackbar.LENGTH_LONG).show();
                } else {
                    startActivity(new Intent(MainActivity.this, VideoUploadActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                }
            }
            if (itemId == R.id.bottom_menu_profile) {
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.video_loading_message), Snackbar.LENGTH_LONG).show();
                } else {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
                        intent.putExtra("profile_user_id", currentUser.getUid());
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    } else {
                        VideoPlayerManager.getInstance().pauseVideo();
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    }
                }
            }
            if (itemId == R.id.bottom_menu_search) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            if (itemId == R.id.bottom_menu_chat) {
                startActivity(new Intent(MainActivity.this, UsersWhoSentMessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }

            return true;
        });
    }

    private void setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_seguindo)));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(getString(R.string.tab_para_ti)));

        loadFragment(new ForYouFragment());
        TabLayout.Tab defaultTab = binding.tabLayout.getTabAt(1);
        if (defaultTab != null) {
            defaultTab.select();
        }

        binding.tabLayout.setSelectedTabIndicatorColor(Color.WHITE);
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        VideoPlayerManager.getInstance().pauseVideo();
                        loadFragment(new FollowingFragment());
                        break;
                    case 1:
                        VideoPlayerManager.getInstance().pauseVideo();
                        loadFragment(new ForYouFragment());
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Handle unselect if needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Handle reselection if needed
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (weakActivity.get() != null) {
            if (VideoPlayerManager.getInstance() != null) {
                VideoPlayerManager.getInstance().releasePlayer();
            }
            weakActivity.clear();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        VideoPlayerManager.getInstance().pauseVideo();
    }
}
