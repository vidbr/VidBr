package com.video.vidbr.fragments;

import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.ForgotPasswordActivity;
import com.video.vidbr.LoginActivity;
import com.video.vidbr.MainActivity;
import com.video.vidbr.R;
import com.video.vidbr.SignupActivity;
import com.video.vidbr.WebViewActivity;
import com.video.vidbr.util.UiUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;

public class BottomSheetLoginFragment extends BottomSheetDialogFragment {

    private EditText emailInput;
    private EditText passwordInput;
    private ImageView showPasswordIcon;
    private Button loginBtn;
    private Button googleLoginBtn;
    private TextView signupBtn;
    private TextView forgotPassword;
    private TextView termsOfServiceText;
    private TextView privacyPolicyText;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    private boolean isPasswordVisible = false; // Flag to track password visibility
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseFirestore firestore;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.ThemeOverlay_App_BottomSheetDialog);
        auth = FirebaseAuth.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_login, container, false);

        firestore = FirebaseFirestore.getInstance();

        emailInput = view.findViewById(R.id.email_input);
        passwordInput = view.findViewById(R.id.password_input);
        showPasswordIcon = view.findViewById(R.id.show_password_icon);
        loginBtn = view.findViewById(R.id.submit_btn);
        googleLoginBtn = view.findViewById(R.id.google_login_btn);
        signupBtn = view.findViewById(R.id.go_to_signup_btn);
        forgotPassword = view.findViewById(R.id.forgotPassword);
        termsOfServiceText = view.findViewById(R.id.terms_of_service_text);
        privacyPolicyText = view.findViewById(R.id.privacy_policy_text);
        progressBar = view.findViewById(R.id.progress_bar);

        loginBtn.setOnClickListener(v -> login());
        signupBtn.setOnClickListener(v -> signup());
        forgotPassword.setOnClickListener(v -> forgotPassword());
        showPasswordIcon.setOnClickListener(v -> togglePasswordVisibility());

        termsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String termsUrl = "https://bit.ly/vidbrtermsandconditions";
                Intent intent = new Intent(getContext(), WebViewActivity.class);
                intent.putExtra("url", termsUrl);
                startActivity(intent);
            }
        });

        privacyPolicyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String privacyUrl = "https://bit.ly/vidbrprivacypolicy";
                Intent intent = new Intent(getContext(), WebViewActivity.class);
                intent.putExtra("url", privacyUrl);
                startActivity(intent);
            }
        });

        googleLoginBtn.setOnClickListener(v -> signInWithGoogle());

        return view;
    }

    private void signup() {
        startActivity(new Intent(getContext(), SignupActivity.class));
    }
    private void forgotPassword() {
        startActivity(new Intent(getContext(), ForgotPasswordActivity.class));
    }

    private void setInProgress(boolean inProgress) {
        progressBar.setVisibility(inProgress ? View.VISIBLE : View.GONE);
        loginBtn.setVisibility(inProgress ? View.GONE : View.VISIBLE);
    }

    private void login() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.email_invalid_error));
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.error_min_password_length));
            return;
        }

        setInProgress(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    UiUtil.showToast(getContext(), getString(R.string.login_success));
                    dismiss();
                })
                .addOnFailureListener(e -> {
                    UiUtil.showToast(getContext(), e.getLocalizedMessage() != null ? e.getLocalizedMessage() : "Something went wrong");
                    setInProgress(false);
                });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            showPasswordIcon.setImageResource(R.drawable.eye);
        } else {
            // Show password
            passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            showPasswordIcon.setImageResource(R.drawable.eye2);
        }
        isPasswordVisible = !isPasswordVisible;
        // Move the cursor to the end of the text
        passwordInput.setSelection(passwordInput.getText().length());
    }

    private void signInWithGoogle() {
        oneTapClient = Identity.getSignInClient(getContext());
        signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        .setServerClientId("899724318789-p53ui69mpgi28m7d6pvksg1pnocc4gpr.apps.googleusercontent.com")
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build();

        oneTapClient.beginSignIn(signInRequest)
                .addOnSuccessListener(ContextCompat.getMainExecutor(getContext()), result -> {
                    try {
                        startIntentSenderForResult(
                                result.getPendingIntent().getIntentSender(),
                                9001, null, 0, 0, 0, null);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Google Sign-In failed", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                                    Toast.makeText(getContext(), "Google authentication failed", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void generateUniqueUsername(String baseUsername, BottomSheetLoginFragment.UsernameCallback callback) {
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
                    Toast.makeText(getContext(), "Erro ao verificar nome de usuário", Toast.LENGTH_SHORT).show();
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
                startActivity(new Intent(getContext(), MainActivity.class));
                getActivity().finish();
            } else {
                // Usuário ainda não existe, gerar um nome de usuário único
                generateUniqueUsername("user", new BottomSheetLoginFragment.UsernameCallback() {
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
                            startActivity(new Intent(getContext(), MainActivity.class));
                            getActivity().finish();
                        }).addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Erro ao salvar dados do usuário", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(getContext(), "Erro ao gerar nome de usuário", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Erro ao verificar conta", Toast.LENGTH_SHORT).show();
        });
    }
}
