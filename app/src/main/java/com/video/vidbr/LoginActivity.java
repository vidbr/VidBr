package com.video.vidbr;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.databinding.ActivityLoginBinding;
import com.video.vidbr.util.UiUtil;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private boolean isPasswordVisible = false; // Flag to track password visibility
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();

        binding.getRoot().setBackgroundColor(getResources().getColor(android.R.color.white));

        FirebaseApp.initializeApp(LoginActivity.this);

        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId("899724318789-p53ui69mpgi28m7d6pvksg1pnocc4gpr.apps.googleusercontent.com")
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        // Google Login Button
        binding.googleLoginBtn.setOnClickListener(v -> signInWithGoogle());

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            finish();
            startActivity(new Intent(this, SplashScreenActivity.class));
            overridePendingTransition(0, 0);
        }

        // Set up password visibility toggle
        binding.showPasswordIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        binding.submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        binding.goToSignupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        binding.forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
                overridePendingTransition(0, 0);
            }
        });

        binding.termsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String termsUrl = "https://bit.ly/vidbrtermsandconditions";
                Intent intent = new Intent(LoginActivity.this, WebViewActivity.class);
                intent.putExtra("url", termsUrl);
                startActivity(intent);
            }
        });

        binding.privacyPolicyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String privacyUrl = "https://bit.ly/vidbrprivacypolicy";
                Intent intent = new Intent(LoginActivity.this, WebViewActivity.class);
                intent.putExtra("url", privacyUrl);
                startActivity(intent);
            }
        });

        binding.googleLoginBtn.setOnClickListener(v -> signInWithGoogle());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            binding.passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.showPasswordIcon.setImageResource(R.drawable.eye);
        } else {
            // Show password
            binding.passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            binding.showPasswordIcon.setImageResource(R.drawable.eye2);
        }
        isPasswordVisible = !isPasswordVisible;
        // Move the cursor to the end of the text
        binding.passwordInput.setSelection(binding.passwordInput.getText().length());
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.submitBtn.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.submitBtn.setVisibility(View.VISIBLE);
        }
    }

    private void login() {
        String email = binding.emailInput.getText().toString();
        String password = binding.passwordInput.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError(getString(R.string.email_invalid_error));
            return;
        }

        loginWithFirebase(email, password);
    }

    private void loginWithFirebase(String email, String password) {
        setInProgress(true);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(
                email,
                password
        ).addOnSuccessListener(authResult -> {
            UiUtil.showToast(LoginActivity.this, getString(R.string.login_success));
            setInProgress(false);
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            overridePendingTransition(0, 0);
            finish();
        }).addOnFailureListener(e -> {
            UiUtil.showToast(getApplicationContext(), e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Algo deu errado");
            setInProgress(false);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar qualquer referência de texto com spans para evitar vazamentos de memória
        if (binding != null) {
            binding.termsAndPrivacy.setText(null); // Limpar o texto com spans clicáveis
        }
        binding = null;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private void signInWithGoogle() {
        oneTapClient = Identity.getSignInClient(this);
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId("899724318789-p53ui69mpgi28m7d6pvksg1pnocc4gpr.apps.googleusercontent.com")
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(this, result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                9001, null, 0, 0, 0, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 9001 && data != null) {
            try {
                SignInCredential credential = oneTapClient.getSignInCredentialFromIntent(data);
                String idToken = credential.getGoogleIdToken();
                if (idToken != null) {
                    AuthCredential firebaseCredential = GoogleAuthProvider.getCredential(idToken, null);
                    FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                                    if (currentUser != null) {
                                        String email = currentUser.getEmail();
                                        String fullName = currentUser.getDisplayName();
                                        String firstName = "";

                                        // Extrair o primeiro nome (se houver)
                                        if (fullName != null && fullName.contains(" ")) {
                                            firstName = fullName.split(" ")[0]; // Pega a primeira palavra antes do espaço
                                        } else {
                                            firstName = fullName; // Caso o nome completo seja apenas um nome
                                        }

                                        String birthday = ""; // Caso o usuário já tenha um campo de aniversário ou queira preenchê-lo mais tarde.

                                        signupWithFirebase(email, firstName, birthday); // Chama o método de cadastro com o primeiro nome
                                    }
                                } else {
                                    Toast.makeText(LoginActivity.this, "Google authentication failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateUniqueUsername(String baseUsername, LoginActivity.UsernameCallback callback) {
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
                    Toast.makeText(LoginActivity.this, "Erro ao verificar nome de usuário", Toast.LENGTH_SHORT).show();
                    callback.onError(e);
                });
    }

    private interface UsernameCallback {
        void onUsernameGenerated(String username);
        void onError(Exception e);
    }

    private void signupWithFirebase(String email, String name, String birthday) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Se o usuário já existe, apenas finalize o login e vá para a MainActivity
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                // Usuário ainda não existe, gerar um nome de usuário único
                generateUniqueUsername("user", new UsernameCallback() {
                    @Override
                    public void onUsernameGenerated(String username) {
                        // Obter o URL da foto de perfil, se disponível
                        String profilePicUrl = currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : "";

                        Map<String, Object> userData = new HashMap<>();
                        userData.put("id", userId);
                        userData.put("name", name);
                        userData.put("birthday", birthday);
                        userData.put("username", username);
                        userData.put("downloadEnabled", true);
                        userData.put("profilePic", profilePicUrl);

                        userRef.set(userData).addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(LoginActivity.this, "Erro ao salvar dados do usuário", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(LoginActivity.this, "Erro ao gerar nome de usuário", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(LoginActivity.this, "Erro ao verificar conta", Toast.LENGTH_SHORT).show();
        });
    }
}
