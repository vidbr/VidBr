package com.video.vidbr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.BeginSignInRequest;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.SignInClient;
import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.databinding.ActivitySignupBinding;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private boolean isPasswordVisible = false; // Flag to track password visibility
    private SignInClient oneTapClient;
    private BeginSignInRequest signInRequest;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firestore = FirebaseFirestore.getInstance();

        // Set up password visibility toggle
        binding.showPasswordIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility();
            }
        });

        binding.submitBtn.setOnClickListener(v -> signup());

        binding.goToLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        binding.termsOfServiceText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String termsUrl = "https://bit.ly/vidbrtermsandconditions";
                Intent intent = new Intent(SignupActivity.this, WebViewActivity.class);
                intent.putExtra("url", termsUrl);
                startActivity(intent);
            }
        });

        binding.privacyPolicyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String privacyUrl = "https://bit.ly/vidbrprivacypolicy";
                Intent intent = new Intent(SignupActivity.this, WebViewActivity.class);
                intent.putExtra("url", privacyUrl);
                startActivity(intent);
            }
        });
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

    private void signup() {
        String email = binding.emailInput.getText().toString();
        String password = binding.passwordInput.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInput.setError(getString(R.string.email_invalid_error));
            return;
        }

        boolean isValidLength = password.length() >= 8 && password.length() <= 20;
        boolean isValidContent = password.matches(".*[a-zA-Z].*") && password.matches(".*[0-9].*");

        if (isValidLength) {
            binding.passwordLengthValidation.setText(getString(R.string.password_length_valid));
        } else {
            binding.passwordLengthValidation.setText(getString(R.string.password_length_invalid));
        }

        if (isValidContent) {
            binding.passwordContentValidation.setText(getString(R.string.password_content_valid));
        } else {
            binding.passwordContentValidation.setText(getString(R.string.password_content_invalid));
        }

        if (!isValidLength || !isValidContent) {
            return;
        }

        // Start the email verification
        setInProgress(true); // Show loading state
        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                setInProgress(false); // Hide loading state

                if (task.isSuccessful()) {
                    // Verifica se a lista de métodos de autenticação para o e-mail está vazia
                    if (task.getResult().getSignInMethods().size() == 0) {
                        // E-mail não registrado, continue com o processo de inscrição
                        Intent intent = new Intent(SignupActivity.this, BirthdayActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("password", password);
                        startActivity(intent);
                        finish();
                    } else {
                        // E-mail já está registrado
                        Toast.makeText(SignupActivity.this, "Email is already registered.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Tratar falhas de verificação, se necessário
                    Toast.makeText(SignupActivity.this, "Failed to verify email.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners to avoid memory leaks
        binding.showPasswordIcon.setOnClickListener(null);
        binding.submitBtn.setOnClickListener(null);
        binding.goToLoginBtn.setOnClickListener(null);
    }
}
