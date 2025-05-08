package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.adapter.MessageAdapter;
import com.video.vidbr.model.MessageModel;
import com.video.vidbr.model.UserModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// IMPORTS — os mesmos do seu código original, sem alterações

// ... (mesmos imports)

public class ChatActivity extends AppCompatActivity implements MessageAdapter.OnMessageLongClickListener {

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<MessageModel> messageList;
    private ImageView buttonBack;
    private EditText editTextMessage;
    private ImageView buttonSend;
    private ProgressBar progressBar;
    private ImageView imageViewProfile;

    private FirebaseAuth mAuth;
    private DatabaseReference db;
    private String currentUserId;
    private String chatUserId;
    private String chatUserName;

    private boolean isLoadingMore = false;
    private String lastLoadedKey = null;
    private String latestMessageKey = null;
    private static final int PAGE_SIZE = 10;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        recyclerView = findViewById(R.id.recycler_view);
        buttonBack = findViewById(R.id.button_back);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);
        progressBar = findViewById(R.id.progress_bar);
        imageViewProfile = findViewById(R.id.image_view_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            currentUserId = mAuth.getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        chatUserId = getIntent().getStringExtra("chatUserId");
        chatUserName = getIntent().getStringExtra("chatUserName");

        if (chatUserId == null || chatUserName == null) {
            finish();
            return;
        }

        TextView textViewChatUserName = findViewById(R.id.text_view_chat_user_name);
        textViewChatUserName.setText(chatUserName);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList, currentUserId, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(messageAdapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (!recyclerView.canScrollVertically(-1) && !isLoadingMore && lastLoadedKey != null) {
                    loadMoreMessages();
                }
            }
        });

        buttonBack.setOnClickListener(v -> finish());
        buttonSend.setOnClickListener(v -> sendMessage());

        imageViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ChatActivity.this, ProfileActivity.class);
            intent.putExtra("profile_user_id", chatUserId);
            startActivity(intent);
        });

        loadInitialMessages();
        loadProfileImage();
        addEmojiListeners();
    }

    private String getChatId(String user1, String user2) {
        if (user1 == null || user2 == null) return null;
        return user1.compareTo(user2) > 0 ? user1 + "_" + user2 : user2 + "_" + user1;
    }

    private void loadInitialMessages() {
        isLoadingMore = true;
        String chatId = getChatId(currentUserId, chatUserId);

        db.child("messages").child(chatId)
                .orderByKey()
                .limitToLast(PAGE_SIZE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MessageModel> tempList = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            MessageModel message = snap.getValue(MessageModel.class);
                            if (message != null) {
                                message.setMessageId(snap.getKey());
                                tempList.add(message);
                            }
                        }
                        if (!tempList.isEmpty()) {
                            lastLoadedKey = tempList.get(0).getMessageId();
                            latestMessageKey = tempList.get(tempList.size() - 1).getMessageId();
                        }
                        messageList.clear();
                        messageList.addAll(tempList);
                        messageAdapter.notifyDataSetChanged();
                        recyclerView.scrollToPosition(messageList.size() - 1);
                        isLoadingMore = false;

                        listenForNewMessages();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoadingMore = false;
                    }
                });
    }

    private void loadMoreMessages() {
        isLoadingMore = true;
        String chatId = getChatId(currentUserId, chatUserId);

        db.child("messages").child(chatId)
                .orderByKey()
                .endBefore(lastLoadedKey)
                .limitToLast(PAGE_SIZE)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<MessageModel> newMessages = new ArrayList<>();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            MessageModel message = snap.getValue(MessageModel.class);
                            if (message != null) {
                                message.setMessageId(snap.getKey());
                                newMessages.add(message);
                            }
                        }
                        if (!newMessages.isEmpty()) {
                            lastLoadedKey = newMessages.get(0).getMessageId();
                            messageList.addAll(0, newMessages);
                            messageAdapter.notifyItemRangeInserted(0, newMessages.size());
                        }
                        isLoadingMore = false;
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        isLoadingMore = false;
                    }
                });
    }

    private void listenForNewMessages() {
        String chatId = getChatId(currentUserId, chatUserId);
        if (chatId == null) return;

        db.child("messages").child(chatId)
                .orderByKey()
                .startAfter(latestMessageKey == null ? "" : latestMessageKey)
                .addChildEventListener(new com.google.firebase.database.ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        MessageModel message = snapshot.getValue(MessageModel.class);
                        if (message != null && snapshot.getKey() != null) {
                            message.setMessageId(snapshot.getKey());
                            messageList.add(message);
                            latestMessageKey = snapshot.getKey();
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            recyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    }

                    @Override public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void sendMessage() {
        String messageText = editTextMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(messageText)) {
            String chatId = getChatId(currentUserId, chatUserId);
            if (chatId != null) {
                MessageModel message = new MessageModel(
                        null, currentUserId, chatUserId,
                        messageText, new Date().getTime(), chatId
                );
                db.child("messages").child(chatId).push().setValue(message)
                        .addOnSuccessListener(aVoid -> editTextMessage.setText(""))
                        .addOnFailureListener(e -> Toast.makeText(ChatActivity.this, "Failed to send", Toast.LENGTH_SHORT).show());
            }
        }
    }

    private void loadProfileImage() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(chatUserId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            UserModel user = doc.toObject(UserModel.class);
                            if (user != null && user.getProfilePic() != null) {
                                Glide.with(ChatActivity.this)
                                        .load(user.getProfilePic())
                                        .placeholder(R.drawable.icon_account_circle)
                                        .error(R.drawable.icon_account_circle)
                                        .circleCrop()
                                        .into(imageViewProfile);
                            }
                        }
                    }
                });
    }

    @Override
    public void onMessageLongClick(MessageModel message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_title))
                .setMessage(getString(R.string.dialog_message))
                .setPositiveButton(getString(R.string.dialog_positive_button), (dialog, which) -> deleteMessage(message))
                .setNegativeButton(getString(R.string.dialog_negative_button), null)
                .show();
    }

    private void deleteMessage(MessageModel message) {
        String chatId = getChatId(currentUserId, chatUserId);
        if (chatId != null && message.getMessageId() != null) {
            db.child("messages").child(chatId).child(message.getMessageId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        // Remover localmente
                        int index = messageList.indexOf(message);
                        if (index != -1) {
                            messageList.remove(index);
                            messageAdapter.notifyItemRemoved(index);
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void addEmojiListeners() {
        int[] emojiIds = {
                R.id.emoji_laughing, R.id.emoji_heart, R.id.emoji_rolling,
                R.id.emoji_heart_eyes, R.id.emoji_pray, R.id.emoji_pleading,
                R.id.emoji_thinking, R.id.emoji_sweat, R.id.emoji_party,
                R.id.emoji_hugging, R.id.emoji_winking
        };
        String[] emojis = {
                "😂", "❤️", "🤣", "😍", "🙏", "🥺", "🤔", "😅", "🎉", "🤗", "😜"
        };
        for (int i = 0; i < emojiIds.length; i++) {
            int finalI = i;
            findViewById(emojiIds[i]).setOnClickListener(v -> {
                String currentText = editTextMessage.getText().toString();
                editTextMessage.setText(currentText + emojis[finalI]);
                editTextMessage.setSelection(editTextMessage.getText().length());
            });
        }
    }
}
