package com.video.vidbr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.tabs.TabLayout;
import com.video.vidbr.databinding.ActivityFollowingBinding;
import com.video.vidbr.fragments.Followers;
import com.video.vidbr.fragments.Following;

public class FollowingActivity extends AppCompatActivity {
    private ActivityFollowingBinding binding;
    private FragmentManager fragmentManager;
    private int tabToSelect = 0; // Default to "Seguindo" tab

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
        }

        // Acesse o TextView diretamente e defina o texto
        TextView toolbarTitle = findViewById(R.id.toolbar_title);
        String userName = getIntent().getStringExtra("current_user_name");
        if (userName != null && !userName.isEmpty()) {
            toolbarTitle.setText(userName);  // Define o nome do usuário como título
        } else {
            toolbarTitle.setText("Atividade");  // Caso o nome não esteja disponível
        }

        fragmentManager = getSupportFragmentManager();

        // Retrieve the tab index to select from the Intent
        Intent intent = getIntent();
        if (intent != null) {
            tabToSelect = intent.getIntExtra("tab_to_select", 0); // Default to "Seguindo" tab
        }

        // Set up TabLayout and Fragment handling
        setupTabs(binding.tabLayout);

        // Set default fragment based on tab index
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, getFragmentForTab(tabToSelect))
                    .commit();
        }

        // Set the tab to the selected index
        binding.tabLayout.getTabAt(tabToSelect).select();
    }

    private void setupTabs(TabLayout tabLayout) {
        tabLayout.addTab(tabLayout.newTab().setText("Seguindo"));
        tabLayout.addTab(tabLayout.newTab().setText("Seguidores"));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                fragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, getFragmentForTab(tab.getPosition()))
                        .commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // No-op
            }
        });
    }

    private Fragment getFragmentForTab(int tabIndex) {
        switch (tabIndex) {
            case 1:
                return new Following();
            case 0:
            default:
                return new Followers();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
