package com.video.vidbr;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.video.vidbr.adapter.VideoListAdapter;
import com.video.vidbr.adapter.VideoPagerAdapter;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.List;

public class VideoPlayerActivity extends AppCompatActivity {

    private static final String TAG = "VideoPlayerActivity";
    private ViewPager2 viewPager2;
    private List<Object> videoList;
    private int startPosition;
    private VideoPagerAdapter videoPagerAdapter;
    private VideoListAdapter videoListAdapter;
    private static final int PAGE_SIZE = 1; // Number of videos to load per page
    private DocumentSnapshot lastVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        viewPager2 = findViewById(R.id.viewPager2);
        videoList = new ArrayList<>();

        // Retrieve the initial position from the intent
        startPosition = getIntent().getIntExtra("start_position", 0);

        // Load videos from Firestore based on the profile type
        loadVideosFromFirebase();
    }

    private void loadVideosFromFirebase() {
        // Retrieve parameters from the intent
        String profileUserIdUser = getIntent().getStringExtra("profile_user_id_user");
        String profileUserIdLiked = getIntent().getStringExtra("profile_user_id_liked");
        String hashtag = getIntent().getStringExtra("profile_user_id_hash");
        String profileUserIdVideo = getIntent().getStringExtra("profile_user_id_video");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        Query query;

        // Set up the query based on input parameters
        if (profileUserIdVideo != null && !profileUserIdVideo.isEmpty()) {
            query = firestore.collection("videos").whereEqualTo("title", profileUserIdVideo);
        } else if (profileUserIdUser != null && !profileUserIdUser.isEmpty()) {
            query = firestore.collection("videos").whereEqualTo("videoId", profileUserIdUser);
        } else if (profileUserIdLiked != null && !profileUserIdLiked.isEmpty()) {
            query = firestore.collection("videos").whereArrayContains("likedBy", profileUserIdLiked);
        } else if (hashtag != null && !hashtag.isEmpty()) {
            query = firestore.collection("videos").whereArrayContains("hashtags", hashtag).orderBy("createdTime", Query.Direction.DESCENDING);
        } else {
            Toast.makeText(this, "Nenhum vídeo encontrado para exibir.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load videos based on the query
        if ((hashtag != null && !hashtag.isEmpty()) || (profileUserIdVideo != null && !profileUserIdVideo.isEmpty())) {
            // Limitar à quantidade de vídeos por página
            query.limit(PAGE_SIZE);

            // Iniciar consulta
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            return;
                        }

                        videoList.clear();

                        // Adicionar vídeos à lista
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            VideoModel video = document.toObject(VideoModel.class);
                            if (video != null) {
                                videoList.add(video);
                            }
                        }

                        // Atualizar o documento "lastVisible"
                        lastVisible = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);

                        // Verificar se o adapter foi inicializado
                        if (videoListAdapter == null) {
                            videoListAdapter = new VideoListAdapter(this, videoList);
                            viewPager2.setAdapter(videoListAdapter);
                        } else {
                            videoListAdapter.updateVideos(videoList);
                        }

                        // Configurar a posição inicial
                        setInitialPosition();
                    })
                    .addOnFailureListener(this::handleFirestoreException);
        } else {
            // For a general list of videos
            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        videoList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            VideoModel video = document.toObject(VideoModel.class);
                            if (video != null) {
                                videoList.add(video);
                            }
                        }
                        //Collections.sort(videoList, (video1, video2) -> video2.getCreatedTime().compareTo(video1.getCreatedTime()));

                        videoPagerAdapter = new VideoPagerAdapter(videoList);
                        viewPager2.setAdapter(videoPagerAdapter);
                        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

                        // Set the current item
                        setInitialPosition();
                    })
                    .addOnFailureListener(this::handleFirestoreException);
        }

        viewPager2.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == videoList.size() - 1) {
                    loadMoreVideosFromFirebase(); // Load more data when reaching the last item
                }
            }
        });
    }

    private void loadMoreVideosFromFirebase() {
        if (lastVisible == null) return;

        String hashtag = getIntent().getStringExtra("profile_user_id_hash");
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Definir a consulta para carregar mais vídeos
        Query query = firestore.collection("videos")
                .whereArrayContains("hashtags", hashtag)
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .startAfter(lastVisible);

        // Carregar mais vídeos
        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    // Adicionar vídeos à lista
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        VideoModel video = document.toObject(VideoModel.class);
                        if (video != null) {
                            videoList.add(video);
                        }
                    }

                    // Atualizar o documento "lastVisible"
                    lastVisible = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.size() - 1);

                    // Notificar o adapter para atualizar a lista
                    videoPagerAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(this::handleFirestoreException);
    }


    private void setInitialPosition() {
        if (startPosition >= 0 && startPosition < viewPager2.getAdapter().getItemCount()) {
            viewPager2.setCurrentItem(startPosition, false);
        }
    }

    private void handleFirestoreException(@NonNull Exception e) {
        String message = "Erro ao carregar vídeos: " + e.getMessage();
        if (e instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                message = "Erro de índice. Tentando novamente em breve...";
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPagerAdapter != null) {
            videoPagerAdapter.pauseCurrentVideo();
        }
        VideoPlayerManager.getInstance().pauseVideo();  // Pause the video using VideoPlayerManager
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (videoPagerAdapter != null) {
            videoPagerAdapter.pauseCurrentVideo();
        }
        VideoPlayerManager.getInstance().pauseVideo();  // Pause the video using VideoPlayerManager
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPagerAdapter != null) {
            videoPagerAdapter.releaseCurrentVideo();
        }
        if (videoListAdapter != null) {
            videoListAdapter.notifyDataSetChanged();
        }
    }
}
