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
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.video.vidbr.R;
import com.video.vidbr.VideoPlayerActivity;
import com.video.vidbr.adapter.ProfileVideoAdapter;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.List;

public class LikedVideosFragment extends Fragment {

    private static final String ARG_PROFILE_USER_ID = "profile_user_id";
    private static final int PAGE_SIZE = 10; // Number of items to load per page

    private RecyclerView recyclerView;
    private ProfileVideoAdapter adapter;
    private List<VideoModel> videoList;
    private String profileUserId;
    private ImageView loading;
    private TextView noVideosMessage; // New TextView for no videos message
    private boolean isLoading = false;
    private DocumentSnapshot lastVisible;
    private ListenerRegistration likedVideosListener;

    public static LikedVideosFragment newInstance(String profileUserId) {
        LikedVideosFragment fragment = new LikedVideosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PROFILE_USER_ID, profileUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_liked_videos, container, false);
        recyclerView = view.findViewById(R.id.recycler_view);
        loading = view.findViewById(R.id.loading);
        noVideosMessage = view.findViewById(R.id.no_videos_message); // Initialize the TextView
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
                .whereArrayContains("likedBy", profileUserId)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (queryDocumentSnapshots.isEmpty()) {
                noVideosMessage.setVisibility(View.VISIBLE); // Show no videos message
            } else {
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                videoList.addAll(queryDocumentSnapshots.toObjects(VideoModel.class));
                adapter.notifyDataSetChanged();
                noVideosMessage.setVisibility(View.GONE); // Hide message
            }
            isLoading = false;
            loading.setVisibility(View.GONE);

            listenForUpdates();
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
                .whereArrayContains("likedBy", profileUserId)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                videoList.addAll(queryDocumentSnapshots.toObjects(VideoModel.class));
                adapter.notifyDataSetChanged();
            }
            isLoading = false;
            loading.setVisibility(View.GONE);

            listenForUpdates();
        }).addOnFailureListener(e -> {
            isLoading = false;
        });
    }

    private void listenForUpdates() {
        if (likedVideosListener != null) return; // Evita adicionar múltiplos listeners

        Query query = FirebaseFirestore.getInstance()
                .collection("videos")
                .whereArrayContains("likedBy", profileUserId)
                .orderBy("createdTime", Query.Direction.DESCENDING);

        likedVideosListener = query.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Error listening for updates: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots != null) {
                boolean listUpdated = false;

                // Iterar pelas mudanças em documentos
                for (DocumentChange documentChange : queryDocumentSnapshots.getDocumentChanges()) {
                    VideoModel video = documentChange.getDocument().toObject(VideoModel.class);
                    switch (documentChange.getType()) {
                        case ADDED:
                            // Verificar se o vídeo já existe na lista com base no ID
                            boolean alreadyExists = false;
                            for (VideoModel existingVideo : videoList) {
                                if (existingVideo.getVideoId().equals(video.getVideoId())) {
                                    alreadyExists = true;
                                    break;
                                }
                            }
                            if (!alreadyExists) {
                                videoList.add(video); // Adicionar na lista
                                listUpdated = true;
                            }
                            break;

                        case MODIFIED:
                            // Atualizar o vídeo modificado na lista
                            for (int i = 0; i < videoList.size(); i++) {
                                if (videoList.get(i).getVideoId().equals(video.getVideoId())) {
                                    videoList.set(i, video);
                                    listUpdated = true;
                                    break;
                                }
                            }
                            break;

                        case REMOVED:
                            // Remover o vídeo da lista
                            videoList.removeIf(existingVideo -> existingVideo.getVideoId().equals(video.getVideoId()));
                            listUpdated = true;
                            break;
                    }
                }

                // Reordenar a lista com base no campo createdTime
                if (listUpdated) {
                    videoList.sort((v1, v2) -> v2.getCreatedTime().compareTo(v1.getCreatedTime())); // Ordena em ordem decrescente
                    adapter.notifyDataSetChanged();
                }

                // Mostrar ou esconder mensagem de "nenhum vídeo"
                if (videoList.isEmpty()) {
                    noVideosMessage.setVisibility(View.VISIBLE);
                } else {
                    noVideosMessage.setVisibility(View.GONE);
                }
            }
        });
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
