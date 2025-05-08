package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.video.vidbr.adapter.VideoGridAdapter;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.List;

public class HashtagVideosActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 10;

    private GridView gridView;
    private VideoGridAdapter adapter;
    private TextView noVideosMessage;
    private TextView hashtagTitle;
    private TextView hashtagVideoCount;

    private String hashtag;
    private List<VideoModel> videos = new ArrayList<>();
    private DocumentSnapshot lastVisible;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtag_videos);

        hashtag = getIntent().getStringExtra("hashtag");

        if (hashtag == null || hashtag.trim().isEmpty()) {
            finish();
            return;
        }

        hashtagTitle = findViewById(R.id.hashtag_title);
        hashtagVideoCount = findViewById(R.id.hashtag_video_count);
        gridView = findViewById(R.id.gridView_videos);
        noVideosMessage = findViewById(R.id.no_videos_message);

        hashtagTitle.setText("#" + hashtag);

        setupGridView();

        countTotalVideos();

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra("profile_user_id_hash", hashtag);
            intent.putExtra("start_position", position);
            startActivity(intent);
            this.overridePendingTransition(0, 0);
        });

        gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                // Não é necessário implementar
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (totalItemCount > 0 && (firstVisibleItem + visibleItemCount >= totalItemCount)) {
                    loadMoreVideos();
                }
            }
        });
    }

    private void setupGridView() {
        videos.clear();
        adapter = new VideoGridAdapter(this, videos);
        gridView.setAdapter(adapter);
        lastVisible = null;
        loadMoreVideos();
    }

    private void loadMoreVideos() {
        Query query = FirebaseFirestore.getInstance().collection("videos")
                .whereArrayContains("hashtags", hashtag)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .whereEqualTo("visibility", "public")
                .limit(PAGE_SIZE);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<VideoModel> newVideos = queryDocumentSnapshots.toObjects(VideoModel.class);

                    if (newVideos.size() > 0) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                    }

                    List<VideoModel> filteredNewVideos = new ArrayList<>();
                    for (VideoModel video : newVideos) {
                        boolean alreadyExists = false;
                        for (VideoModel existingVideo : videos) {
                            if (video.getId().equals(existingVideo.getId())) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        if (!alreadyExists) {
                            filteredNewVideos.add(video);
                        }
                    }

                    if (filteredNewVideos.size() == 0 && videos.size() == 0) {
                        noVideosMessage.setVisibility(View.VISIBLE);
                        gridView.setVisibility(View.GONE);
                    } else {
                        noVideosMessage.setVisibility(View.GONE);
                        gridView.setVisibility(View.VISIBLE);
                        videos.addAll(filteredNewVideos);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    noVideosMessage.setText("Erro ao carregar vídeos.");
                    noVideosMessage.setVisibility(View.VISIBLE);
                    gridView.setVisibility(View.GONE);
                });
    }

    private void countTotalVideos() {
        FirebaseFirestore.getInstance().collection("videos")
                .whereArrayContains("hashtags", hashtag)
                .whereEqualTo("visibility", "public")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalVideosCount = queryDocumentSnapshots.size();
                    hashtagVideoCount.setText(totalVideosCount + " " + getString(R.string.video_count_label));
                })
                .addOnFailureListener(e -> {
                    hashtagVideoCount.setText("Erro ao contar vídeos.");
                });
    }

    public void tagVoltar(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
