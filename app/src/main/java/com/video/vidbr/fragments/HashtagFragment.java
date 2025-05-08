package com.video.vidbr.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.video.vidbr.R;
import com.video.vidbr.adapter.HashtagListAdapter;
import com.video.vidbr.model.HashtagModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HashtagFragment extends Fragment {

    private RecyclerView hashtagResultsList;
    private HashtagListAdapter hashtagAdapter;
    private List<HashtagModel> hashtagResults;
    private LinearLayout noResultsLayout;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_hashtag, container, false);

        hashtagResultsList = view.findViewById(R.id.hashtag_results_list);
        noResultsLayout = view.findViewById(R.id.no_results_layout);

        db = FirebaseFirestore.getInstance();
        if (db == null) {
        }

        hashtagResults = new ArrayList<>();
        hashtagAdapter = new HashtagListAdapter(getContext(), hashtagResults);
        hashtagResultsList.setAdapter(hashtagAdapter);
        hashtagResultsList.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String query = getArguments() != null ? getArguments().getString("query") : "";

        if (!query.isEmpty()) {
            performHashtagSearch(query);
        }
    }

    public void performHashtagSearch(String query) {
        if (db == null) {
            return;
        }

        db.collection("videos")
                .limit(10)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        
                        Set<String> uniqueHashtags = new HashSet<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> hashtags = (List<String>) document.get("hashtags");
                            if (hashtags != null) {
                                for (String hashtag : hashtags) {
                                    uniqueHashtags.add(hashtag);
                                }
                            }
                        }

                        List<HashtagModel> allHashtags = new ArrayList<>();
                        for (String uniqueHashtag : uniqueHashtags) {
                            HashtagModel hashtagModel = new HashtagModel();
                            hashtagModel.setName(uniqueHashtag);
                            allHashtags.add(hashtagModel);
                        }

                        List<HashtagModel> filteredResults = new ArrayList<>();
                        for (HashtagModel hashtag : allHashtags) {
                           if (hashtag.getName().startsWith(query)) {
                                filteredResults.add(hashtag);
                            }
                        }

                        updateResults(filteredResults);
                    } else {
                        Toast.makeText(getContext(), "Error getting documents: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    public void updateResults(List<HashtagModel> results) {
        if (getContext() == null) {
            return;
        }

        hashtagResults.clear();
        hashtagResults.addAll(results);
        hashtagAdapter.notifyDataSetChanged();

        if (hashtagResults.isEmpty()) {
            noResultsLayout.setVisibility(View.VISIBLE);
        } else {
            noResultsLayout.setVisibility(View.GONE);
        }
    }

}
