package com.video.vidbr.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.video.vidbr.R;
import com.video.vidbr.VideoPlayerActivity;
import com.video.vidbr.adapter.ProfileVideoAdapter;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserVideosFragment extends Fragment {

    private static final String ARG_PROFILE_USER_ID = "profile_user_id";
    private static final int PAGE_SIZE = 10;

    private RecyclerView recyclerView;
    private ProfileVideoAdapter adapter;
    private List<VideoModel> videoList;
    private String profileUserId;
    private ImageView loading;
    private TextView noVideosMessage;
    private boolean isLoading = false;
    private DocumentSnapshot lastVisible;
    private Map<String, Integer> videoPositionMap = new HashMap<>();

    public static UserVideosFragment newInstance(String profileUserId) {
        UserVideosFragment fragment = new UserVideosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_USER_ID, profileUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_videos, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        loading = view.findViewById(R.id.loading);
        noVideosMessage = view.findViewById(R.id.no_videos_message);

        Glide.with(view.getContext()).asGif().load(R.drawable.loading).into(loading);

        if (getArguments() != null) {
            profileUserId = getArguments().getString(ARG_PROFILE_USER_ID);
        }

        if (profileUserId == null) {
            throw new IllegalArgumentException("Profile User ID must be provided");
        }

        videoList = new ArrayList<>();
        setupRecyclerView();
        fetchInitialData();
        return view;
    }

    private void setupRecyclerView() {
        adapter = new ProfileVideoAdapter(videoList, getContext());
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(position -> {
            VideoModel selectedVideo = videoList.get(position);
            Intent intent = new Intent(requireActivity(), VideoPlayerActivity.class);
            intent.putExtra("profile_user_id_user", selectedVideo.getVideoId());
            intent.putExtra("start_position", position);
            startActivity(intent);
            requireActivity().overridePendingTransition(0, 0);
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1) && !isLoading) {
                    noVideosMessage.setVisibility(View.GONE);
                    fetchMoreData();
                }
            }
        });
    }

    private void fetchInitialData() {
        isLoading = true;
        Query query = FirebaseFirestore.getInstance()
                .collection("videos")
                .whereEqualTo("uploaderId", profileUserId)
                .whereEqualTo("visibility", "public")
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                noVideosMessage.setVisibility(View.VISIBLE);
            } else {
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                videoList.addAll(queryDocumentSnapshots.toObjects(VideoModel.class));
                adapter.notifyDataSetChanged();
                noVideosMessage.setVisibility(View.GONE);

                for (VideoModel video : videoList) {
                    setupVisibilityListener(video.getVideoId());
                }
            }
            isLoading = false;
            loading.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchMoreData() {
        if (lastVisible == null || isLoading) return;

        isLoading = true;
        loading.setVisibility(View.VISIBLE);

        Query query = FirebaseFirestore.getInstance()
                .collection("videos")
                .whereEqualTo("uploaderId", profileUserId)
                .whereEqualTo("visibility", "public")
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
                List<VideoModel> newVideos = queryDocumentSnapshots.toObjects(VideoModel.class);

                for (VideoModel newVideo : newVideos) {
                    if (!videoList.contains(newVideo)) {
                        videoList.add(newVideo);
                    }
                }

                adapter.notifyDataSetChanged();

                for (VideoModel video : videoList) {
                    setupVisibilityListener(video.getVideoId());
                }
            }
            isLoading = false;
            loading.setVisibility(View.GONE);
        }).addOnFailureListener(e -> {
            isLoading = false;
        });
    }

    private void setupVisibilityListener(String videoId) {
        FirebaseFirestore.getInstance()
                .collection("videos")
                .document(videoId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) {
                        return;
                    }

                    if (snapshot != null && snapshot.exists()) {
                        String newVisibility = snapshot.getString("visibility");

                        if (newVisibility != null) {
                            updateVideoVisibility(videoId, newVisibility, snapshot.toObject(VideoModel.class));
                        }
                    } else {
                        // Video has been deleted
                        removeVideoFromList(videoId);
                    }
                });
    }

    private void removeVideoFromList(String videoId) {
        for (int i = 0; i < videoList.size(); i++) {
            if (videoList.get(i).getVideoId().equals(videoId)) {
                videoList.remove(i);
                adapter.notifyItemRemoved(i);

                // Update empty state message if no videos left
                if (videoList.isEmpty()) {
                    noVideosMessage.setVisibility(View.VISIBLE);
                }
                break;
            }
        }
    }

    private void updateVideoVisibility(String videoId, String newVisibility, VideoModel updatedVideo) {
        if ("private".equals(newVisibility)) {
            for (int i = 0; i < videoList.size(); i++) {
                if (videoList.get(i).getVideoId().equals(videoId)) {
                    videoPositionMap.put(videoId, i); // Salva a posição antes de remover
                    videoList.remove(i);
                    adapter.notifyItemRemoved(i);
                    break;
                }
            }
        } else if ("public".equals(newVisibility)) {
            boolean videoExists = false;

            // Verifica se o vídeo já está na lista
            for (VideoModel video : videoList) {
                if (video.getVideoId().equals(videoId)) {
                    videoExists = true;
                    break;
                }
            }

            if (!videoExists && updatedVideo != null) {
                Integer originalPosition = videoPositionMap.get(videoId);

                if (originalPosition != null && originalPosition < videoList.size()) {
                    // Adiciona de volta à posição original
                    videoList.add(originalPosition, updatedVideo);
                    adapter.notifyItemInserted(originalPosition);
                } else {
                    // Adiciona ao final, caso a posição original não seja válida
                    videoList.add(updatedVideo);
                    adapter.notifyItemInserted(videoList.size() - 1);
                }
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isLoading && videoList.isEmpty()) {
            isLoading = true;
            loading.setVisibility(View.VISIBLE);
            fetchInitialData();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isLoading) {
            loading.setVisibility(View.GONE);
        }
    }
}
