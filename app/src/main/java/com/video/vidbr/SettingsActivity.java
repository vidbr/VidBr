package com.video.vidbr;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsActivity extends AppCompatActivity {
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle("");
        }

        TextView textAccount = findViewById(R.id.text_account);
        TextView textProfile = findViewById(R.id.text_profile);
        TextView textPrivacy = findViewById(R.id.text_privacy);
        TextView textShare = findViewById(R.id.text_share);
        TextView textAbout = findViewById(R.id.text_about);
        TextView textLogout = findViewById(R.id.text_logout);

        textAccount.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AccountActivity.class);
            startActivity(intent);
        });

        textProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        textPrivacy.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, PrivacyActivity.class);
            startActivity(intent);
        });

        // Carregar nome de usuário de Firestore e preparar texto de compartilhamento
        textShare.setOnClickListener(v -> {
            // Verificar se o usuário está autenticado
            String currentUserId = auth.getCurrentUser().getUid();
            firestore.collection("users").document(currentUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            if (username != null) {
                                // Usar a string de "Confira meu perfil em: " do strings.xml
                                String shareTextPrefix = getString(R.string.share_text_prefix);
                                String shareText = shareTextPrefix + "\n\nhttps://vidbr.com.br/profile.html?username=" + username;

                                // Iniciar a intenção de compartilhamento
                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("text/plain");
                                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

                                startActivity(Intent.createChooser(shareIntent, "Compartilhar via"));
                            }
                        }
                    });
        });

        textAbout.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
            startActivity(intent);
        });

        textLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
