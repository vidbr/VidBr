package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;

import com.video.vidbr.adapter.UserAdapter;
import com.video.vidbr.model.UserModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsersWhoSentMessagesActivity extends AppCompatActivity {

    private RecyclerView recyclerViewUsers;
    private UserAdapter userAdapter;
    private List<UserModel> userList;
    private ImageView progressBar;
    private TextView textNoMessages;

    private DatabaseReference dbRealtime;
    private FirebaseFirestore dbFirestore;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private Set<String> collectedUserIds = new HashSet<>();

    private static final int PAGE_SIZE = 10;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private DocumentSnapshot lastVisibleUser = null;
    private boolean isUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_who_sent_message);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("");
        }

        recyclerViewUsers = findViewById(R.id.recycler_view_users);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));

        textNoMessages = findViewById(R.id.text_no_messages);

        userList = new ArrayList<>();
        userAdapter = new UserAdapter(this, userList);
        recyclerViewUsers.setAdapter(userAdapter);

        mAuth = FirebaseAuth.getInstance();
        dbRealtime = FirebaseDatabase.getInstance().getReference();
        dbFirestore = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        dbFirestore.setFirestoreSettings(settings);

        BottomNavigationView bottomNavBar = findViewById(R.id.bottom_nav_bar);
        bottomNavBar.setItemIconTintList(null);

        Menu menu = bottomNavBar.getMenu();
        MenuItem chatItem = menu.findItem(R.id.bottom_menu_chat);
        chatItem.setIcon(R.drawable.icon_chat);

        bottomNavBar.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.bottom_menu_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
            } else if (itemId == R.id.bottom_menu_add_video) {
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading_video_message), Snackbar.LENGTH_LONG).show();
                } else {
                    startActivity(new Intent(this, VideoUploadActivity.class));
                    overridePendingTransition(0, 0);
                }
            } else if (itemId == R.id.bottom_menu_profile) {
                if (isUploading) {
                    Snackbar.make(findViewById(android.R.id.content), getString(R.string.loading_video_message), Snackbar.LENGTH_LONG).show();
                } else {
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    if (currentUser != null) {
                        Intent intent = new Intent(this, ProfileActivity.class);
                        intent.putExtra("profile_user_id", currentUser.getUid());
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    } else {
                        VideoPlayerManager.getInstance().pauseVideo();
                        startActivity(new Intent(this, LoginActivity.class));
                    }
                }
            } else if (itemId == R.id.bottom_menu_search) {
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }

            return true;
        });

        recyclerViewUsers.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading && !isLastPage) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadNextPage();
                    }
                }
            }
        });

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
            loadUsersWhoSentOrReceivedMessages();
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void loadUsersWhoSentOrReceivedMessages() {
        dbRealtime.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Set<String> userIds = new HashSet<>();
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                        String senderId = messageSnapshot.child("senderId").getValue(String.class);
                        String receiverId = messageSnapshot.child("receiverId").getValue(String.class);

                        if (senderId != null && receiverId != null) {
                            if (receiverId.equals(currentUserId)) {
                                userIds.add(senderId);
                            } else if (senderId.equals(currentUserId)) {
                                userIds.add(receiverId);
                            }
                        }
                    }
                }
                collectedUserIds = userIds;
                fetchUserDetails(collectedUserIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(UsersWhoSentMessagesActivity.this, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchUserDetails(Set<String> userIds) {
        if (userIds.isEmpty()) {
            recyclerViewUsers.setVisibility(View.GONE);
            textNoMessages.setVisibility(View.VISIBLE);
            return;
        }

        isLoading = true;

        Query query = dbFirestore.collection("users")
                .whereIn("id", new ArrayList<>(userIds))
                .limit(PAGE_SIZE);

        if (lastVisibleUser != null) {
            query = query.startAfter(lastVisibleUser);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                List<DocumentSnapshot> documents = queryDocumentSnapshots.getDocuments();
                lastVisibleUser = documents.get(documents.size() - 1);

                for (DocumentSnapshot document : documents) {
                    UserModel user = document.toObject(UserModel.class);
                    if (user != null) {
                        userList.add(user);
                        fetchLastMessage(user.getId(), user);
                    }
                }

                userAdapter.notifyDataSetChanged();
                recyclerViewUsers.setVisibility(View.VISIBLE);
                textNoMessages.setVisibility(View.GONE);

                if (documents.size() < PAGE_SIZE) {
                    isLastPage = true;
                }
            } else {
                if (userList.isEmpty()) {
                    recyclerViewUsers.setVisibility(View.GONE);
                    textNoMessages.setVisibility(View.VISIBLE);
                }
                isLastPage = true;
            }

            isLoading = false;
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(this, "Erro ao carregar usuários", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadNextPage() {
        if (!isLastPage && !isLoading) {
            isLoading = true;
            fetchUserDetails(collectedUserIds);
        }
    }

    private void fetchLastMessage(String userId, UserModel user) {
        dbRealtime.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String lastMessage = null;
                boolean isReceived = false;
                long lastMessageTimestamp = 0;

                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                        String senderId = messageSnapshot.child("senderId").getValue(String.class);
                        String receiverId = messageSnapshot.child("receiverId").getValue(String.class);
                        String message = messageSnapshot.child("message").getValue(String.class);
                        Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);

                        if ((senderId.equals(userId) || receiverId.equals(userId)) && message != null) {
                            lastMessage = message;
                            isReceived = receiverId.equals(currentUserId);
                            if (timestamp != null) {
                                lastMessageTimestamp = timestamp;
                            }
                        }
                    }
                }

                if (lastMessage != null && lastMessage.length() > 30) {
                    lastMessage = lastMessage.substring(0, 30) + "...";
                }

                user.setLastMessage(lastMessage);
                user.setLastMessageReceived(isReceived);
                user.setLastMessageTimestamp(lastMessageTimestamp);
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
}
