package com.video.vidbr.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.video.vidbr.LoginActivity;
import com.video.vidbr.R;
import com.video.vidbr.adapter.VideoListAdapter;
import com.video.vidbr.model.UserModel;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.List;

public class FollowingFragment extends Fragment {
    private static final int PAGE_SIZE = 10; // Number of items per page

    private VideoListAdapter adapter;
    private String profileUserId;
    private List<String> followingList;
    private List<Object> videoList = new ArrayList<>(); // List to hold videos and ads
    private TextView textViewMessage;
    private Button loginBtn;
    private DocumentSnapshot lastVisible;
    private boolean isLoading = false;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_following, container, false);
        viewPager = view.findViewById(R.id.view_pager);
        textViewMessage = view.findViewById(R.id.textViewMessage);
        loginBtn = view.findViewById(R.id.loginBtn);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            profileUserId = currentUser.getUid();
            fetchFollowingList();
        } else {
            textViewMessage.setText(R.string.login_prompt);
            textViewMessage.setVisibility(View.VISIBLE);
            loginBtn.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
        }

        // Register a page change callback to fetch more data when near the end
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!isLoading && position >= (adapter.getItemCount() - 1) * 0.8) {
                    fetchMoreVideos();
                }
            }
        });

        return view;
    }

    private void fetchFollowingList() {
        FirebaseFirestore.getInstance().collection("users")
                .document(profileUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel currentUser = documentSnapshot.toObject(UserModel.class);
                    if (currentUser != null) {
                        followingList = currentUser.getFollowingList();
                        if (followingList == null || followingList.isEmpty()) {
                            textViewMessage.setText(R.string.following_list_empty);
                            textViewMessage.setVisibility(View.VISIBLE);
                            viewPager.setVisibility(View.GONE);
                        } else {
                            setupVideoQuery();
                        }
                    } else {
                        textViewMessage.setText("Error fetching user data.");
                        textViewMessage.setVisibility(View.VISIBLE);
                        viewPager.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    textViewMessage.setText("Error fetching following list.");
                    textViewMessage.setVisibility(View.VISIBLE);
                    viewPager.setVisibility(View.GONE);
                });
    }

    private void setupVideoQuery() {
        Query query = FirebaseFirestore.getInstance()
                .collection("videos")
                .whereIn("uploaderId", followingList)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .whereEqualTo("visibility", "public")
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    VideoModel videoModel = document.toObject(VideoModel.class);
                    if (videoModel != null) {
                        videoList.add(videoModel);
                    }
                }
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
                setupVideoList();
            } else {
                textViewMessage.setText(R.string.no_videos_found);
                textViewMessage.setVisibility(View.VISIBLE);
                viewPager.setVisibility(View.GONE);
            }
        }).addOnFailureListener(e -> {
            textViewMessage.setText("Error fetching initial data.");
            textViewMessage.setVisibility(View.VISIBLE);
            viewPager.setVisibility(View.GONE);
        });
    }

    private void setupVideoList() {
        adapter = new VideoListAdapter(getContext(), videoList);
        viewPager.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        textViewMessage.setVisibility(View.GONE);
        viewPager.setVisibility(View.VISIBLE);
    }

    private void fetchMoreVideos() {
        if (isLoading || followingList == null || followingList.isEmpty()) return;

        if (lastVisible == null) {
            return;
        }

        isLoading = true;

        Query query = FirebaseFirestore.getInstance()
                .collection("videos")
                .whereIn("uploaderId", followingList)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .whereEqualTo("visibility", "public")
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    VideoModel videoModel = document.toObject(VideoModel.class);
                    if (videoModel != null) {
                        videoList.add(videoModel);
                    }
                }
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
                adapter.notifyDataSetChanged();
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            isLoading = false;
            textViewMessage.setText("Error fetching more videos.");
            textViewMessage.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.notifyDataSetChanged(); // Update adapter if needed
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.notifyDataSetChanged(); // Optional: release resources if needed
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }

        videoList.clear();
    }
}
