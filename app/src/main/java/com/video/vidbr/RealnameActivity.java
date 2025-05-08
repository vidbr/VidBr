package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RealnameActivity extends AppCompatActivity {

    private EditText nameTextView;
    private Button confirmButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realname);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        nameTextView = findViewById(R.id.birthday_text);
        confirmButton = findViewById(R.id.confirm_button);

        String email = getIntent().getStringExtra("email");
        String password = getIntent().getStringExtra("password");
        String birthday = getIntent().getStringExtra("birthday");

        confirmButton.setOnClickListener(v -> {
            String name = nameTextView.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(RealnameActivity.this, getString(R.string.msg_insira_nome), Toast.LENGTH_SHORT).show();
                return;
            }

            signupWithFirebase(email, password, name, birthday);
        });
    }

    private void generateUniqueUsername(String baseUsername, UsernameCallback callback) {
        int randomSuffix = new Random().nextInt(1_000_000_000);
        String formattedSuffix = String.format("%09d", randomSuffix);
        String generatedUsername = baseUsername + formattedSuffix;

        firestore.collection("users")
                .whereEqualTo("username", generatedUsername)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onUsernameGenerated(generatedUsername);
                    } else {
                        generateUniqueUsername(baseUsername, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RealnameActivity.this, "Erro ao verificar nome de usuário", Toast.LENGTH_SHORT).show();
                    callback.onError(e);
                });
    }

    private interface UsernameCallback {
        void onUsernameGenerated(String username);
        void onError(Exception e);
    }

    private void signupWithFirebase(String email, String password, String name, String birthday) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                    if (currentUser != null) {
                        String userId = currentUser.getUid();
                        DocumentReference userRef = firestore.collection("users").document(userId);

                        generateUniqueUsername("user", new UsernameCallback() {
                            @Override
                            public void onUsernameGenerated(String username) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("id", userId);
                                userData.put("name", name);
                                userData.put("birthday", birthday);
                                userData.put("username", username);
                                userData.put("downloadEnabled", true);

                                userRef.set(userData).addOnSuccessListener(aVoid -> {
                                    Toast.makeText(RealnameActivity.this, getString(R.string.account_created_success), Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(RealnameActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(RealnameActivity.this, "Erro ao salvar dados do usuário", Toast.LENGTH_SHORT).show();
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(RealnameActivity.this, "Erro ao gerar nome de usuário", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RealnameActivity.this, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
