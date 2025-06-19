package com.video.vidbr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.video.vidbr.adapter.SuggestionAdapter;
import com.video.vidbr.adapter.ViewPagerAdapter;
import com.video.vidbr.fragments.HashtagFragment;
import com.video.vidbr.fragments.UserFragment;
import com.video.vidbr.fragments.VideoFragment;
import com.video.vidbr.model.HashtagModel;
import com.video.vidbr.util.UiUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SearchActivity extends AppCompatActivity {

    private SearchView searchInput;
    private FirebaseFirestore db;

    private VideoFragment videoFragment;
    private UserFragment userFragment;
    private HashtagFragment hashtagFragment;


    private String userCountry;
    private RecyclerView suggestionRecyclerView;
    private SuggestionAdapter suggestionAdapter;
    private List<String> suggestionList;
    private ViewPager2 viewPager;
    private boolean isUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        Locale locale = Locale.getDefault();
        userCountry = locale.getDisplayCountry(Locale.ENGLISH);

        searchInput = findViewById(R.id.search_input);
        db = FirebaseFirestore.getInstance();

        viewPager = findViewById(R.id.view_pager);
        TabLayout tabLayout = findViewById(R.id.tab_layout);

        videoFragment = new VideoFragment();
        userFragment = new UserFragment();
        hashtagFragment = new HashtagFragment();

        suggestionRecyclerView = findViewById(R.id.suggestion_recycler_view);
        suggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionList = new ArrayList<>();
        suggestionAdapter = new SuggestionAdapter(suggestionList, suggestion -> {
            searchInput.setQuery(suggestion, true);
            suggestionRecyclerView.setVisibility(View.GONE);
            viewPager.setVisibility(View.VISIBLE);
        });
        suggestionRecyclerView.setAdapter(suggestionAdapter);

        ViewPagerAdapter adapter = new ViewPagerAdapter(this);
        adapter.addFragment(videoFragment, getString(R.string.video_tab));
        adapter.addFragment(userFragment, getString(R.string.user_tab));
        adapter.addFragment(hashtagFragment, getString(R.string.hashtag_tab));

        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(adapter.getFragmentTitle(position))
        ).attach();

        // Customize SearchView
        customizeSearchView();
        searchInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                TextView viewMoreHistoryText = findViewById(R.id.view_more_history_text);

                if (viewMoreHistoryText != null) {
                    if (query.isEmpty()) {
                        viewMoreHistoryText.setVisibility(View.VISIBLE);
                    } else {
                        viewMoreHistoryText.setVisibility(View.GONE);
                    }
                }


                if (!query.isEmpty()) {
                    if (hashtagFragment != null) {
                        Bundle args = new Bundle();
                        args.putString("query", query);
                        hashtagFragment.setArguments(args);
                        hashtagFragment.performHashtagSearch(query);
                    }

                    videoFragment.addSearchToHistory(query);
                    videoFragment.performVideoSearch2(query);
                    userFragment.searchUsers(query);
                    suggestionRecyclerView.setVisibility(View.GONE);
                    viewPager.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(SearchActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    videoFragment.updateSearchResultsVisibility(false);  
                }

                if (!newText.isEmpty()) {
                    fetchSearchSuggestions(newText);
                    suggestionAdapter.setQuery(newText);
                } else {
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                    suggestionRecyclerView.setVisibility(View.GONE);
                    viewPager.setVisibility(View.VISIBLE);
                }
                return false;
            }

        });

        BottomNavigationView bottomNavBar = findViewById(R.id.bottom_nav_bar);
        bottomNavBar.setItemIconTintList(null); // Set icon tint

        Menu menu = bottomNavBar.getMenu();
        MenuItem searchItem = menu.findItem(R.id.bottom_menu_search);

        searchItem.setIcon(R.drawable.search);

        bottomNavBar.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.bottom_menu_home) {
                startActivity(new Intent(SearchActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (itemId == R.id.bottom_menu_add_video) {
                // Handle video upload
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading_video_message), Snackbar.LENGTH_LONG).show();
                } else {
                    startActivity(new Intent(SearchActivity.this, VideoUploadActivity.class));
                    overridePendingTransition(0, 0);
                }
            } else if (itemId == R.id.bottom_menu_profile) {
                // Handle profile navigation
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading_video_message), Snackbar.LENGTH_LONG).show();
                } else {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        Intent intent = new Intent(SearchActivity.this, ProfileActivity.class);
                        intent.putExtra("profile_user_id", currentUser.getUid());
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    } else {
                        VideoPlayerManager.getInstance().pauseVideo();
                        startActivity(new Intent(SearchActivity.this, LoginActivity.class));
                    }
                }
            } else if (itemId == R.id.bottom_menu_search) {

            } else if (itemId == R.id.bottom_menu_chat) {
                startActivity(new Intent(SearchActivity.this, UsersWhoSentMessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }

            return true;
        });
    }

    public void updateSearchView(String query) {
        if (searchInput != null) {
            searchInput.setQuery(query, false);
        }

        userFragment.searchUsers(query);
        if (hashtagFragment != null) {
            Bundle args = new Bundle();
            args.putString("query", query);
            hashtagFragment.setArguments(args);
            hashtagFragment.performHashtagSearch(query);
        }
    }

    private void customizeSearchView() {
        ImageView searchHintIcon = searchInput.findViewById(androidx.appcompat.R.id.search_mag_icon);
        searchHintIcon.setColorFilter(Color.BLACK);

        EditText searchEditText = searchInput.findViewById(androidx.appcompat.R.id.search_src_text);
        searchEditText.setTextColor(getResources().getColor(R.color.black));
        searchEditText.setHintTextColor(getResources().getColor(R.color.gray));
    }

    private void fetchSearchSuggestions(String query) {
        db.collection("videos")
                .orderBy("title")
                .startAt(query)
                .endAt(query + '\uf8ff')
                .whereEqualTo("country", userCountry)
                .whereEqualTo("visibility", "public")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Set<String> uniqueSuggestions = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String title = document.getString("title");
                            if (title != null) {
                                uniqueSuggestions.add(title);
                            }
                        }
                        suggestionList.clear();
                        suggestionList.addAll(uniqueSuggestions);
                        suggestionAdapter.notifyDataSetChanged();
                        suggestionRecyclerView.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                        viewPager.setVisibility(suggestionList.isEmpty() ? View.VISIBLE : View.GONE); // Toggle ViewPager visibility
                    } else {
                        Toast.makeText(SearchActivity.this, "Error getting suggestions: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching data", Toast.LENGTH_SHORT).show();
					Log.e("ErrorFD", "Error fetching data", e);
                });
    }
}
