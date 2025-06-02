package com.video.vidbr;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.video.vidbr.adapter.LikedUsersAdapter;
import com.video.vidbr.model.UserModel;

import java.util.ArrayList;
import java.util.List;

public class LikedUsersActivity extends AppCompatActivity {

    private RecyclerView likedUsersRecyclerView;
    private LikedUsersAdapter likedUsersAdapter;
    private List<UserModel> likedUsersList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_liked_users);

        likedUsersRecyclerView = findViewById(R.id.likedUsersRecyclerView);
        likedUsersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String videoId = getIntent().getStringExtra("videoId");
        db = FirebaseFirestore.getInstance();

        likedUsersAdapter = new LikedUsersAdapter(LikedUsersActivity.this, likedUsersList);
        likedUsersRecyclerView.setAdapter(likedUsersAdapter);

        loadLikedUsers(videoId);
    }

    private void loadLikedUsers(String videoId) {

        db.collection("videos").document(videoId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            List<String> likedBy = (List<String>) document.get("likedBy");

                            if (likedBy != null && !likedBy.isEmpty()) {
                                int limit = Math.min(likedBy.size(), 10);

                                for (int i = 0; i < limit; i++) {
                                    String userId = likedBy.get(i);
                                    loadUserData(userId);
                                }
                            } else {
                               Toast.makeText(LikedUsersActivity.this, "Nenhum usuário encontrou que curtiu este vídeo.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        Toast.makeText(LikedUsersActivity.this, "Erro ao carregar dados!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadUserData(String userId) {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot snapshot = task.getResult();
                        if (snapshot != null && snapshot.exists()) {
                            UserModel user = snapshot.toObject(UserModel.class);
                            if (user != null) {
                                likedUsersList.add(user); // Adiciona o usuário na lista
                            }
                        }
                    } else {
                      Toast.makeText(LikedUsersActivity.this, "Erro ao carregar dados do usuário!", Toast.LENGTH_SHORT).show();
                    }

                    likedUsersAdapter.notifyDataSetChanged();
                });
    }
}
