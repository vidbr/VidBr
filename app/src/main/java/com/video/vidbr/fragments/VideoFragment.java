package com.video.vidbr.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.video.vidbr.SearchActivity;
import com.video.vidbr.adapter.HistoryAdapter;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.video.vidbr.HashtagVideosActivity;
import com.video.vidbr.R;
import com.video.vidbr.VideoPlayerActivity;
import com.video.vidbr.adapter.VideoGridAdapter;
import com.video.vidbr.adapter.CategoryAdapter;
import com.video.vidbr.model.VideoModel;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import android.content.Intent;

public class VideoFragment extends Fragment {

    private GridView searchResultsGrid;
    private ListView categoriesListView, searchHistoryListView;
    private VideoGridAdapter resultsAdapter;
    private CategoryAdapter categoryAdapter;
    private List<VideoModel> searchResults;
    private List<String> categories;
    private List<String> searchHistory;
    private TextView viewMoreHistoryText;
    private ArrayAdapter<String> historyAdapter;
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private boolean hasMoreResults = true;

    private static final String PREFS_NAME = "SearchHistoryPrefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";

    private TextView topicsText;

    private LinearLayout noResultsLayout;
    private Handler handler = new Handler();
    private Runnable runnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);

        searchResultsGrid = view.findViewById(R.id.search_results_grid);
        categoriesListView = view.findViewById(R.id.categories_list_view);
        noResultsLayout = view.findViewById(R.id.no_results_layout);
        topicsText = view.findViewById(R.id.topics_text);
        searchHistoryListView = view.findViewById(R.id.search_history_list_view);
        viewMoreHistoryText = view.findViewById(R.id.view_more_history_text);

        db = FirebaseFirestore.getInstance();

        searchResults = new ArrayList<>();
        resultsAdapter = new VideoGridAdapter(getContext(), searchResults);
        searchResultsGrid.setAdapter(resultsAdapter);

        searchHistory = loadSearchHistory();
        historyAdapter = new HistoryAdapter(getContext(), viewMoreHistoryText);
        searchHistoryListView.setAdapter(historyAdapter);

        if (searchHistory.isEmpty()) {
            searchHistoryListView.setVisibility(View.GONE);
        } else {
            searchHistoryListView.setVisibility(View.VISIBLE);
        }

        searchHistoryListView.setOnItemClickListener((parent, view13, position, id) -> {
            String searchQuery = searchHistory.get(position);
            performVideoSearch(searchQuery);

            if (getActivity() instanceof SearchActivity) {
                SearchActivity activity = (SearchActivity) getActivity();
                activity.updateSearchView(searchQuery);
            }
        });

        if (searchHistory.size() > 4) {
            viewMoreHistoryText.setVisibility(View.VISIBLE);
        } else {
            viewMoreHistoryText.setVisibility(View.GONE);
        }

        categories = Arrays.asList(
                getString(R.string.category_music),
                getString(R.string.category_love),
                getString(R.string.category_meme),
                getString(R.string.category_indirect),
                getString(R.string.category_faith),
                getString(R.string.category_sad),
                getString(R.string.category_humor),
                getString(R.string.category_free_fire),
                getString(R.string.category_funny),
                getString(R.string.category_motor),
                getString(R.string.category_games),
                getString(R.string.category_otaku),
                getString(R.string.category_dance),
                getString(R.string.category_motivation),
                getString(R.string.category_football),
                getString(R.string.category_greetings),
                getString(R.string.category_makeup)
        );

        categoryAdapter = new CategoryAdapter(getContext(), categories);
        categoriesListView.setAdapter(categoryAdapter);

        Map<String, String> categoryToHashtagMap = new HashMap<>();
        categoryToHashtagMap.put(getString(R.string.category_music), getString(R.string.hashtag_music));
        categoryToHashtagMap.put(getString(R.string.category_love), getString(R.string.hashtag_love));
        categoryToHashtagMap.put(getString(R.string.category_meme), getString(R.string.hashtag_meme));
        categoryToHashtagMap.put(getString(R.string.category_indirect), getString(R.string.hashtag_indirect));
        categoryToHashtagMap.put(getString(R.string.category_faith), getString(R.string.hashtag_faith));
        categoryToHashtagMap.put(getString(R.string.category_sad), getString(R.string.hashtag_sad));
        categoryToHashtagMap.put(getString(R.string.category_humor), getString(R.string.hashtag_humor));
        categoryToHashtagMap.put(getString(R.string.category_free_fire), getString(R.string.hashtag_free_fire));
        categoryToHashtagMap.put(getString(R.string.category_funny), getString(R.string.hashtag_funny));
        categoryToHashtagMap.put(getString(R.string.category_motor), getString(R.string.hashtag_motor));
        categoryToHashtagMap.put(getString(R.string.category_games), getString(R.string.hashtag_games));
        categoryToHashtagMap.put(getString(R.string.category_otaku), getString(R.string.hashtag_otaku));
        categoryToHashtagMap.put(getString(R.string.category_dance), getString(R.string.hashtag_dance));
        categoryToHashtagMap.put(getString(R.string.category_motivation), getString(R.string.hashtag_motivation));
        categoryToHashtagMap.put(getString(R.string.category_football), getString(R.string.hashtag_football));
        categoryToHashtagMap.put(getString(R.string.category_greetings), getString(R.string.hashtag_greetings));
        categoryToHashtagMap.put(getString(R.string.category_makeup), getString(R.string.hashtag_makeup));

        categoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = categories.get(position);
                String hashtag = categoryToHashtagMap.get(selectedCategory);

                if (hashtag != null) {
                    Intent intent = new Intent(getContext(), HashtagVideosActivity.class);
                    intent.putExtra("hashtag", hashtag);
                    startActivity(intent);
                }
            }
        });

        searchResultsGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                VideoModel selectedVideo = searchResults.get(position);
                String profileUserId = selectedVideo.getTitle(); // Ajuste conforme necessário

                Intent intent = new Intent(requireActivity(), VideoPlayerActivity.class);
                intent.putExtra("profile_user_id_video", profileUserId);
                intent.putExtra("start_position", position);
                startActivity(intent);
                requireActivity().overridePendingTransition(0, 0);
            }
        });

        searchResultsGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Não faz nada
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount >= totalItemCount - 1 && hasMoreResults && !isLoading) {
                    loadVideos();
                }
            }
        });

        return view;
    }

    private void saveSearchHistory() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String jsonHistory = gson.toJson(searchHistory);
        editor.putString(KEY_SEARCH_HISTORY, jsonHistory);
        editor.apply();
    }

   private List<String> loadSearchHistory() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String jsonHistory = sharedPreferences.getString(KEY_SEARCH_HISTORY, "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(jsonHistory, type);
    }


    public void addSearchToHistory(String searchQuery) {
        List<String> tempHistory = new ArrayList<>(searchHistory);

        if (!tempHistory.contains(searchQuery)) {
            tempHistory.add(0, searchQuery);  // Adiciona no topo
        }

        if (tempHistory.size() > 10) {
            tempHistory.remove(tempHistory.size() - 1);  // Remove o item mais antigo
        }

        searchHistory.clear();
        searchHistory.addAll(tempHistory);

        historyAdapter = new HistoryAdapter(getContext(),viewMoreHistoryText);
        searchHistoryListView.setAdapter(historyAdapter);

        saveSearchHistory();
        historyAdapter.notifyDataSetChanged();

        if (!searchHistory.isEmpty()) {
            searchHistoryListView.setVisibility(View.VISIBLE);
            if (searchHistory.size() > 4) {
                viewMoreHistoryText.setVisibility(View.VISIBLE);
            } else {
                viewMoreHistoryText.setVisibility(View.GONE);
            }
        }
    }

    public void updateSearchResultsVisibility(boolean showResults) {
        if (showResults && !searchResults.isEmpty()) {
            searchResultsGrid.setVisibility(View.VISIBLE);
            categoriesListView.setVisibility(View.VISIBLE);
            topicsText.setVisibility(View.VISIBLE);
            noResultsLayout.setVisibility(View.GONE);
        } else {
            searchHistoryListView.setVisibility(View.VISIBLE);
            searchResultsGrid.setVisibility(View.GONE);
            categoriesListView.setVisibility(View.VISIBLE);
            topicsText.setVisibility(View.VISIBLE);
            noResultsLayout.setVisibility(View.GONE);

            if (searchHistory.size() > 4) {
                viewMoreHistoryText.setVisibility(View.VISIBLE);
            } else {
                viewMoreHistoryText.setVisibility(View.GONE);
            }
        }
    }

    public void updateResults(List<VideoModel> results) {
        searchResults.clear();
        searchResults.addAll(results);
        resultsAdapter.notifyDataSetChanged();

        if (searchResults.isEmpty()) {
            searchResultsGrid.setVisibility(View.GONE);
            categoriesListView.setVisibility(View.GONE);
            topicsText.setVisibility(View.GONE);
            noResultsLayout.setVisibility(View.VISIBLE);
        } else {
            searchResultsGrid.setVisibility(View.VISIBLE);
            categoriesListView.setVisibility(View.GONE);
            topicsText.setVisibility(View.GONE);
            noResultsLayout.setVisibility(View.GONE);
        }
    }

    public void performVideoSearch(String query) {

        searchResults.clear();
        resultsAdapter.notifyDataSetChanged();
        lastVisible = null;
        hasMoreResults = true;

        String start = query;
        String end = query + '\uf8ff';

        db.collection("videos")
                .orderBy("title")
                .whereEqualTo("visibility", "public")
                .startAt(start)
                .endAt(end)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<VideoModel> searchResults = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            VideoModel video = document.toObject(VideoModel.class);
                            searchResults.add(video);
                        }
                        updateResults(searchResults);

                        if (!searchResults.isEmpty()) {
                            lastVisible = task.getResult().getDocuments().get(task.getResult().size() - 1);
                        } else {
                            hasMoreResults = false;
                        }

                        //addSearchToHistory(query);
                        searchHistoryListView.setVisibility(View.GONE);
                        viewMoreHistoryText.setVisibility(View.GONE);
                    } else {
                        Toast.makeText(getContext(), "Error getting documents: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void performVideoSearch2(String query) {
        viewMoreHistoryText.setText(getContext().getString(R.string.view_more_history));
        addSearchToHistory(query);

        searchResults.clear();
        resultsAdapter.notifyDataSetChanged();
        lastVisible = null;
        hasMoreResults = true;

        String start = query;
        String end = query + '\uf8ff';

        runnable = () -> {
            db.collection("videos")
                    .orderBy("title")
                    .whereEqualTo("visibility", "public")
                    .startAt(start)
                    .endAt(end)
                    .limit(10)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<VideoModel> searchResults = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                VideoModel video = document.toObject(VideoModel.class);
                                searchResults.add(video);
                            }
                            updateResults(searchResults);

                            if (!searchResults.isEmpty()) {
                                lastVisible = task.getResult().getDocuments().get(task.getResult().size() - 1);
                            } else {
                                hasMoreResults = false;
                            }

                            //addSearchToHistory(query);
                            searchHistoryListView.setVisibility(View.GONE);
                            viewMoreHistoryText.setVisibility(View.GONE);
                        } else {
                            Toast.makeText(getContext(), "Error getting documents: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        };
            handler.postDelayed(runnable, 300);
    }

    private void loadVideos() {
        isLoading = true;

        if (lastVisible == null || !hasMoreResults) {
            isLoading = false;
            return;
        }

        db.collection("videos")
                .orderBy("title")
                .whereEqualTo("visibility", "public")
                .startAfter(lastVisible)
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<VideoModel> newResults = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            VideoModel video = document.toObject(VideoModel.class);
                            newResults.add(video);
                        }

                        if (!newResults.isEmpty()) {
                            lastVisible = task.getResult().getDocuments().get(task.getResult().size() - 1);
                            searchResults.addAll(newResults);
                            resultsAdapter.notifyDataSetChanged();
                        } else {
                            hasMoreResults = false;
                        }
                    } else {
                        Toast.makeText(getContext(), "Error getting documents: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    isLoading = false;
                });
    }
}
