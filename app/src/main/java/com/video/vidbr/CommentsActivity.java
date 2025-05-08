package com.video.vidbr;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.adapter.CommentsAdapter;
import com.video.vidbr.model.CommentModel;
import com.video.vidbr.model.UserModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CommentsAdapter commentsAdapter;
    private List<CommentModel> commentList;
    private EditText commentEditText;
    private ImageView sendButton;
    private String videoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        // Get video ID from intent
        videoId = getIntent().getStringExtra("video_id");

        recyclerView = findViewById(R.id.recyclerView_comments);
        commentEditText = findViewById(R.id.editText_comment);
        sendButton = findViewById(R.id.imageView_send);

        commentList = new ArrayList<>();
        commentsAdapter = new CommentsAdapter(commentList, videoId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(commentsAdapter);

        loadComments();

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addComment();
            }
        });
    }

    private void loadComments() {
        // Load comments from Firestore based on video ID
        FirebaseFirestore.getInstance().collection("videos")
                .document(videoId)
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    commentList.clear();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        CommentModel comment = document.toObject(CommentModel.class);
                        if (comment != null) {
                            // Recuperar nome de usuário associado ao ID do usuário no comentário
                            FirebaseFirestore.getInstance().collection("users")
                                    .document(comment.getUserId())
                                    .get()
                                    .addOnSuccessListener(userDocument -> {
                                        UserModel user = userDocument.toObject(UserModel.class);
                                        if (user != null) {
                                            comment.setUsername(user.getUsername());
                                            comment.setProfilePic(user.getProfilePic());
                                            commentList.add(comment);
                                            commentsAdapter.notifyDataSetChanged();
                                        }
                                    });
                        }
                    }
                });
    }

    private void addComment() {
        String commentText = commentEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(commentText)) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                Date commentDate = new Date();
                long timestamp = commentDate.getTime();
                String commentId = FirebaseFirestore.getInstance().collection("videos")
                        .document(videoId)
                        .collection("comments").document().getId(); // Gera um novo ID

                // Get user information
                FirebaseFirestore.getInstance().collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null) {
                                String username = user.getUsername();
                                String profilePic = user.getProfilePic();

                                CommentModel newComment = new CommentModel(commentId, userId, username, profilePic, commentText, commentDate, timestamp);

                                // Add new comment to Firestore
                                FirebaseFirestore.getInstance().collection("videos")
                                        .document(videoId)
                                        .collection("comments")
                                        .document(commentId) // Salva com o ID gerado
                                        .set(newComment)
                                        .addOnSuccessListener(aVoid -> {
                                            // Comment added successfully
                                            commentEditText.setText(""); // Clear input field after adding comment
                                            // Update comment count
                                            updateCommentCount();
                                            Toast.makeText(CommentsActivity.this, "Comment added successfully", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle failure to add comment
                                            Toast.makeText(CommentsActivity.this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        });
            }
        } else {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCommentCount() {
        // Update comment count in Firestore document of the video
        FirebaseFirestore.getInstance().collection("videos")
                .document(videoId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int currentCommentCount = documentSnapshot.getLong("commentCount") != null ? documentSnapshot.getLong("commentCount").intValue() : 0;
                        // Increment comment count
                        currentCommentCount++;
                        // Update comment count in Firestore
                        documentSnapshot.getReference().update("commentCount", currentCommentCount);
                    }
                });
    }
}
