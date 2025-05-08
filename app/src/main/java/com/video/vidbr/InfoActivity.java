package com.video.vidbr;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle("");
        }

        TextView emailTextView = findViewById(R.id.text_view_email);
        TextView emailDescTextView = findViewById(R.id.text_view_email_desc);
        TextView dobTextView = findViewById(R.id.text_view_dob);
        TextView regionTextView = findViewById(R.id.text_view_region);
        ImageView iconError = findViewById(R.id.icon_error);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String email = user.getEmail();
            String[] emailParts = email.split("@");
            String maskedEmail = emailParts[0].substring(0, 2) + "***@" + emailParts[1];
            emailTextView.setText(getString(R.string.email_label) + maskedEmail);

            if (user.isEmailVerified()) {
                iconError.setVisibility(View.GONE);
                emailDescTextView.setText(getString(R.string.email_verified));
            } else {
                iconError.setVisibility(View.VISIBLE);
                emailDescTextView.setText(getString(R.string.email_not_verified));
            }

            emailTextView.setOnClickListener(v -> {
                showEmailVerificationDialog(this);
            });
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (user != null) {
            String userId = user.getUid();
            DocumentReference docRef = db.collection("users").document(userId);

            docRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot document) {
                    if (document.exists()) {
                        String birthday = document.getString("birthday");
                        dobTextView.setText(getString(R.string.birthday_label) + birthday);
                    }
                }
            });
        }

        Locale locale = Locale.getDefault();
        String userCountry = locale.getDisplayCountry(Locale.ENGLISH);
        regionTextView.setText(getString(R.string.region_label) + userCountry);
    }

    private void showEmailVerificationDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_email_verification, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.alert_dialog_background);
        dialog.show();

        TextView tvVerifyEmail = dialogView.findViewById(R.id.tvVerifyEmail);
        TextView tvEmailInfo = dialogView.findViewById(R.id.tvEmailInfo);
        Button btnVerifyEmail = dialogView.findViewById(R.id.btnVerifyEmail);
        Button btnChangeEmail = dialogView.findViewById(R.id.btnChangeEmail);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null && user.isEmailVerified()) {
            tvVerifyEmail.setText(getString(R.string.email_da_conta));
            tvEmailInfo.setText(getString(R.string.informacao_email));
            btnVerifyEmail.setVisibility(View.GONE);
        } else {
            tvVerifyEmail.setText(R.string.verify_email);
            tvEmailInfo.setText(R.string.email_info);
            btnVerifyEmail.setVisibility(View.VISIBLE);
        }

        btnVerifyEmail.setOnClickListener(v -> {
            if (user != null) {
                user.sendEmailVerification()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, context.getString(R.string.email_verification_sent), Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(context, "Falha ao enviar e-mail de verificação. Tente novamente.", Toast.LENGTH_SHORT).show();
                        });
            }
            dialog.dismiss();
        });

        btnChangeEmail.setOnClickListener(v -> {
            showEmailChangeDialog(this);
            dialog.dismiss();
        });
    }

    private void showEmailChangeDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_email, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.alert_dialog_background);
        dialog.show();

        EditText edtNewEmail = dialogView.findViewById(R.id.edtNewEmail);
        Button btnSubmitNewEmail = dialogView.findViewById(R.id.btnSubmitNewEmail);

        btnSubmitNewEmail.setOnClickListener(v -> {
            String newEmail = edtNewEmail.getText().toString().trim();

            // Basic email validation
            if (newEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                edtNewEmail.setError(getString(R.string.email_error));
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                user.updateEmail(newEmail)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, context.getString(R.string.email_changed_successfully), Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("FirebaseError", "Erro ao atualizar o e-mail: " + e.getMessage());
                            Toast.makeText(context, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
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
