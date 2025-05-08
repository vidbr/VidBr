package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.video.vidbr.model.UserModel;

import java.util.ArrayList;
import java.util.List;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DeleteAccount extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private DatabaseReference database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.delete_account);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        Button confirmButton = findViewById(R.id.confirm_button);
        Button cancelButton = findViewById(R.id.cancel_button);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteUserAccount();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void deleteUserComments(String userId) {
        db.collection("videos").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                for (QueryDocumentSnapshot videoDoc : task.getResult()) {
                    String videoId = videoDoc.getId();

                    db.collection("videos").document(videoId)
                            .collection("comments")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(commentsSnapshot -> {
                                int commentsToDelete = commentsSnapshot.size(); // Quantidade de comentários a serem excluídos

                                if (commentsToDelete > 0) {
                                    // Deleta os comentários
                                    for (DocumentSnapshot commentDoc : commentsSnapshot.getDocuments()) {
                                        commentDoc.getReference().delete();
                                    }

                                    // Atualiza o commentCount no Firestore
                                    db.collection("videos").document(videoId).get()
                                            .addOnSuccessListener(videoSnapshot -> {
                                                if (videoSnapshot.exists() && videoSnapshot.contains("commentCount")) {
                                                    long currentCommentCount = videoSnapshot.getLong("commentCount");
                                                    long newCommentCount = Math.max(0, currentCommentCount - commentsToDelete);

                                                    db.collection("videos").document(videoId)
                                                            .update("commentCount", newCommentCount)
                                                            .addOnSuccessListener(aVoid ->
                                                                    System.out.println("commentCount atualizado para " + newCommentCount))
                                                            .addOnFailureListener(e ->
                                                                    System.out.println("Erro ao atualizar commentCount: " + e.getMessage()));
                                                }
                                            });
                                }
                            });
                }
            }
        });
    }

    private void deleteChatsForCurrentUser() {
        String userId = auth.getCurrentUser().getUid();

        // Referência à coleção "messages" no Firebase Realtime Database
        database.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Lista para armazenar os chatIds
                List<String> chatIds = new ArrayList<>();

                // Loop para percorrer as mensagens
                for (DataSnapshot chatSnapshot : dataSnapshot.getChildren()) {
                    // Iterando sobre as mensagens de cada chat
                    for (DataSnapshot messageSnapshot : chatSnapshot.getChildren()) {
                        String senderId = messageSnapshot.child("senderId").getValue(String.class);
                        String receiverId = messageSnapshot.child("receiverId").getValue(String.class);

                        // Verifica se o usuário está envolvido no chat
                        if (senderId != null && senderId.equals(userId) || receiverId != null && receiverId.equals(userId)) {
                            String chatId = messageSnapshot.child("chatId").getValue(String.class);
                            if (chatId != null && !chatIds.contains(chatId)) {
                                chatIds.add(chatId); // Adiciona o chatId à lista
                            }
                        }
                    }
                }

                // Agora excluir os chats
                if (chatIds.isEmpty()) {
                   // Nenhum chat encontrado para excluir
                } else {
                    deleteChats(chatIds); // Passa a lista de chatIds para excluir
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(DeleteAccount.this, "Erro ao buscar chats.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteChats(List<String> chatIds) {
        // Itera sobre cada chatId e remove do Firebase
        for (String chatId : chatIds) {
            database.child("messages").child(chatId).removeValue();
        }
    }

    private void deleteUserAccount() {
        String userId = auth.getCurrentUser().getUid();

        deleteChatsForCurrentUser();
        removeUserFromFollowersAndFollowing(userId);
        deleteUserComments(userId);

        // Buscar vídeos no Firestore
        db.collection("videos").whereEqualTo("uploaderId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String videoId = document.getId();
                            String videoUrl = document.getString("videoUrl"); // Obtém a URL do vídeo no Firestore

                            if (videoUrl != null && videoUrl.contains("/videos/")) {
                                // Extrai o nome do arquivo do vídeo
                                String fileName = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);

                                // Excluir o vídeo do BunnyCDN
                                deleteVideoFromBunnyCDN(fileName, () -> {
                                    // Após deletar do BunnyCDN, remover do Firestore
                                    db.collection("videos").document(videoId).delete();
                                });
                            }
                        }
                        // Após deletar os vídeos, deletar a foto de perfil do usuário
                        deleteProfilePicture(userId);
                    } else {
                        Toast.makeText(DeleteAccount.this, "Erro ao buscar vídeos.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Método para excluir vídeo do BunnyCDN via requisição DELETE
     */
    private void deleteVideoFromBunnyCDN(String fileName, Runnable onSuccess) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String storageZone = StorageConfig.STORAGE_ZONE; // Substitua pelo nome da Storage Zone no BunnyCDN
                String apiKey = StorageConfig.API_KEY; // Substitua pela sua AccessKey do BunnyCDN
                String urlStr = "https://storage.bunnycdn.com/" + storageZone + "/videos/" + fileName;

                URL url = new URL(urlStr);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("AccessKey", apiKey);
                connection.setDoOutput(true);

                int responseCode = connection.getResponseCode();
                String responseMsg = connection.getResponseMessage();

               if (responseCode == HttpURLConnection.HTTP_NO_CONTENT || responseCode == HttpURLConnection.HTTP_OK) {
                    // Vídeo deletado com sucesso, executa o callback
                    runOnUiThread(onSuccess);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void removeUserFromFollowersAndFollowing(String userId) {
        // Remove user from following list of other users
        db.collection("users").get().addOnCompleteListener(usersTask -> {
            if (usersTask.isSuccessful()) {
                for (DocumentSnapshot userDoc : usersTask.getResult()) {
                    UserModel userModel = userDoc.toObject(UserModel.class);

                    // If the user is following the user being deleted, remove the user
                    if (userModel != null && userModel.getFollowingList() != null && userModel.getFollowingList().contains(userId)) {
                        userModel.getFollowingList().remove(userId);

                        // Update the user's following list in Firestore
                        db.collection("users").document(userDoc.getId()).set(userModel)
                                .addOnCompleteListener(updateFollowingTask -> {
                                    if (!updateFollowingTask.isSuccessful()) {
                                        Toast.makeText(DeleteAccount.this, "Erro ao atualizar lista de seguindo.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }

                    // If the user is followed by the user being deleted, remove the user
                    if (userModel != null && userModel.getFollowerList() != null && userModel.getFollowerList().contains(userId)) {
                        userModel.getFollowerList().remove(userId);

                        // Update the user's follower list in Firestore
                        db.collection("users").document(userDoc.getId()).set(userModel)
                                .addOnCompleteListener(updateFollowersTask -> {
                                    if (!updateFollowersTask.isSuccessful()) {
                                        Toast.makeText(DeleteAccount.this, "Erro ao atualizar lista de seguidores.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                }
            } else {
                Toast.makeText(DeleteAccount.this, "Erro ao buscar usuários.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteProfilePicture(String userId) {
        // Referência ao documento do usuário
        DocumentReference userDocRef = db.collection("users").document(userId);

        // Buscar o link da imagem do campo "profilePic"
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    // Recuperar o link da imagem do campo "profilePic"
                    String profilePicUrl = document.getString("profilePic");

                    if (profilePicUrl != null) {
                        // Extrair o nome do arquivo da URL (por exemplo, "1742237627808.webp")
                        String fileName = profilePicUrl.substring(profilePicUrl.lastIndexOf("/") + 1);

                        // Chamar o método para deletar a imagem do BunnyCDN
                        deleteFromBunnyCDN(StorageConfig.STORAGE_ZONE, StorageConfig.API_KEY, fileName);
                    }
                }
            }
        });

        // Agora delete o documento do usuário da coleção "users"
        userDocRef.delete()
                .addOnCompleteListener(deleteTask -> {
                    if (deleteTask.isSuccessful()) {
                        // Agora delete o usuário do Firebase
                        deleteFirebaseUser();
                    } else {
                        Toast.makeText(DeleteAccount.this, "Erro ao excluir documento do usuário.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void deleteFromBunnyCDN(String storageZone, String apiKey, String fileName) {
        // Use ExecutorService para executar a operação de rede em um thread de segundo plano
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String deleteUrl = "https://storage.bunnycdn.com/" + storageZone + "/profilePic/" + fileName;
                HttpURLConnection deleteConnection = (HttpURLConnection) new URL(deleteUrl).openConnection();
                deleteConnection.setRequestMethod("DELETE");
                deleteConnection.setRequestProperty("AccessKey", apiKey);

                int deleteResponseCode = deleteConnection.getResponseCode();
                if (deleteResponseCode == HttpURLConnection.HTTP_NO_CONTENT || deleteResponseCode == HttpURLConnection.HTTP_OK) {
                   //Imagem deletada com sucesso
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteFirebaseUser() {
        auth.getCurrentUser().delete().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(DeleteAccount.this, R.string.account_deleted_success, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DeleteAccount.this, LoginActivity.class));
                finish();
            } else {
                Toast.makeText(DeleteAccount.this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
