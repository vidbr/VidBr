package com.video.vidbr;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.model.UserModel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView profilePic;
    private EditText editNameTop;
    private EditText editUsername;
    private EditText editBio;
    private EditText editBirthday;
    private Button btnSave;
    private Button btnChangePhoto;
    private Uri tempSelectedPhotoUri = null;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String profileUserId;
    private String currentUserId;
    private Calendar myCalendar = Calendar.getInstance();
    TextView textUsernameError;
    private boolean isUsernameAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle("");
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        profileUserId = getIntent().getStringExtra("profile_user_id");
        currentUserId = mAuth.getCurrentUser().getUid();

        profilePic = findViewById(R.id.profile_pic);
        editNameTop = findViewById(R.id.edit_name_top);
        editUsername = findViewById(R.id.edit_username);
        editBio = findViewById(R.id.edit_bio);
        editBirthday = findViewById(R.id.edit_birthday);
        btnSave = findViewById(R.id.btn_save);
        btnChangePhoto = findViewById(R.id.btn_change_photo);

        profilePic.setOnClickListener(v -> selectProfilePhoto());

        TextView textNameCharCount = findViewById(R.id.text_name_char_count);
        TextView textUsernameCharCount = findViewById(R.id.text_username_char_count);
        textUsernameError = findViewById(R.id.text_username_error);
        TextView textCharCount = findViewById(R.id.text_char_count);

        editNameTop.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                int remainingNameChars = 20 - editable.length();
                textNameCharCount.setText(remainingNameChars + "/20");
            }
        });

        editUsername.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                String username = editable.toString().trim();

                if (username.length() < 4) {
                    textUsernameError.setText(getString(R.string.username_error_message));
                    textUsernameError.setTextColor(Color.parseColor("#000000"));
                    textUsernameError.setVisibility(View.VISIBLE);
                    isUsernameAvailable = false;
                } else if (!isUsernameValidFormat(username)) {
                    textUsernameError.setText(getString(R.string.error_invalid_username));
                    textUsernameError.setTextColor(Color.parseColor("#FF0000"));
                    textUsernameError.setVisibility(View.VISIBLE);
                    isUsernameAvailable = false;
                } else {
                    checkUsernameAvailability(username);
                }

                int remainingUsernameChars = 20 - editable.length();
                textUsernameCharCount.setText(remainingUsernameChars + "/20");
            }
        });

        editBio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                int remainingChars = 80 - editable.length();
                textCharCount.setText(remainingChars + "/80");
            }
        });

        btnChangePhoto.setOnClickListener(v -> selectProfilePhoto());

        btnSave.setOnClickListener(v -> saveChanges());

        editBirthday.setOnClickListener(v -> showDatePickerDialog());

        loadUserProfile();
    }

    private boolean isUsernameValidFormat(String username) {
        return username.matches("^[a-z0-9._]+$");
    }

    private void showDatePickerDialog() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            myCalendar.set(Calendar.YEAR, year);
            myCalendar.set(Calendar.MONTH, month);
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            updateBirthdayLabel();
        }, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateBirthdayLabel() {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        editBirthday.setText(sdf.format(myCalendar.getTime()));
    }

    private void loadUserProfile() {
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        UserModel userModel = documentSnapshot.toObject(UserModel.class);
                        if (userModel != null) {
                            editNameTop.setText(userModel.getName());
                            editUsername.setText(userModel.getUsername());
                            editBio.setText(userModel.getBio());
                            if (userModel.getBirthday() != null) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                    Calendar cal = Calendar.getInstance();
                                    cal.setTime(sdf.parse(userModel.getBirthday()));
                                    myCalendar.setTime(cal.getTime());
                                    updateBirthdayLabel();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            Glide.with(this)
                                    .load(userModel.getProfilePic())
                                    .circleCrop()
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(R.drawable.icon_account_circle)
                                    .into(profilePic);
                        }
                    }
                });
    }

    private void selectProfilePhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 100); // Código de solicitação para a seleção de imagem
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null && data.getData() != null) {
            tempSelectedPhotoUri = data.getData();
            // Atualizar a visualização da imagem selecionada temporariamente
            Glide.with(this)
                    .load(tempSelectedPhotoUri)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.icon_account_circle)
                    .into(profilePic);
        }
    }

    private void compressImageWithFFmpeg(Uri photoUri) {
        String inputPath = getRealPathFromURI(photoUri);
        if (inputPath == null) {
            return;
        }

        // Cria o arquivo de saída com a extensão .webp
        File compressedImageFile = new File(getCacheDir(), "compressed_image.webp");
        String outputPath = compressedImageFile.getAbsolutePath();

        String[] command = {
                "-i", inputPath,
                "-vf", "scale=640:-1", // Reduz largura para 640px mantendo proporção
                "-c:v", "libwebp",      // Converte para WebP (melhor compressão)
                "-q:v", "50",           // Ajusta qualidade (0-100, onde 50 é um bom equilíbrio)
                "-preset", "picture",   // Otimiza para imagens estáticas
                "-map_metadata", "-1",  // Remove metadados desnecessários
                outputPath            // Usa diretamente o caminho correto
        };

        FFmpeg.executeAsync(command, (executionId, returnCode) -> {
            if (returnCode == RETURN_CODE_SUCCESS) {
                Uri compressedImageUri = Uri.fromFile(compressedImageFile);
                uploadToBunnyCDN(compressedImageUri, compressedImageFile, StorageConfig.STORAGE_ZONE, StorageConfig.API_KEY);
            }
        });
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = cursor.getString(column_index);
            cursor.close();
            return path;
        }
        return null;
    }

    private void uploadToBunnyCDN(Uri photoUri, File compressedImageFile, String storageZone, String apiKey) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference userDocRef = db.collection("users").document(userId);

        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            // Gerar nome único para o novo arquivo
            String fileName = System.currentTimeMillis() + ".webp"; // Nome único baseado no timestamp
            String oldFileName;

            if (documentSnapshot.exists() && documentSnapshot.contains("profilePic")) {
                String currentProfilePicUrl = documentSnapshot.getString("profilePic");
                if (currentProfilePicUrl != null && !currentProfilePicUrl.isEmpty()) {
                    oldFileName = Uri.parse(currentProfilePicUrl).getLastPathSegment(); // Nome do arquivo antigo
                } else {
                    oldFileName = null;
                }
            } else {
                oldFileName = null;
            }

            ExecutorService executor = Executors.newSingleThreadExecutor();
            String finalFileName = fileName;
            executor.execute(() -> {
                try {
                    // Deletar a imagem antiga se ela existir
                    if (oldFileName != null) {
                        String deleteUrl = "https://storage.bunnycdn.com/" + storageZone + "/profilePic/" + oldFileName;
                        HttpURLConnection deleteConnection = (HttpURLConnection) new URL(deleteUrl).openConnection();
                        deleteConnection.setRequestMethod("DELETE");
                        deleteConnection.setRequestProperty("AccessKey", apiKey);

                        int deleteResponseCode = deleteConnection.getResponseCode();
                        if (deleteResponseCode == HttpURLConnection.HTTP_NO_CONTENT || deleteResponseCode == HttpURLConnection.HTTP_OK) {
                          //Imagem antiga deletada com sucesso
                        }
                    }

                    // Upload da nova imagem com novo nome
                    String urlStr = "https://storage.bunnycdn.com/" + storageZone + "/profilePic/" + finalFileName;
                    HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
                    connection.setRequestMethod("PUT");
                    connection.setRequestProperty("AccessKey", apiKey);
                    connection.setRequestProperty("Content-Type", "image/webp");
                    connection.setDoOutput(true);

                    try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(compressedImageFile));
                         BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        String imageUrl = "https://my-videos-2.b-cdn.net/profilePic/" + finalFileName;
                        // Atualiza a URL da nova imagem no Firestore
                        userDocRef.update("profilePic", imageUrl)
                                .addOnSuccessListener(aVoid -> {

                                })
                                .addOnFailureListener(e -> Log.e("FirestoreError", "Erro ao atualizar URL no Firestore: " + e.getMessage()));

                        // Deleta o arquivo comprimido local após o upload, se o arquivo já existir, excluí-lo
                        if (compressedImageFile.exists()) {
                            compressedImageFile.delete();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });
        }).addOnFailureListener(e -> Log.e("FirestoreError", "Erro ao obter profilePic do Firestore: " + e.getMessage()));
    }

    private void checkUsernameAvailability(String username) {
        db.collection("users")
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (DocumentSnapshot document : task.getResult()) {
                                String existingUserId = document.getId();
                                if (existingUserId.equals(currentUserId)) {
                                    textUsernameError.setText(getString(R.string.username_already_used_error));
                                    textUsernameError.setTextColor(Color.parseColor("#000000"));
                                    textUsernameError.setVisibility(View.VISIBLE);
                                    isUsernameAvailable = true;  // Nome de usuário disponível
                                } else {
                                    textUsernameError.setText(getString(R.string.username_in_use_error));
                                    textUsernameError.setTextColor(Color.parseColor("#FF0000"));
                                    textUsernameError.setVisibility(View.VISIBLE);
                                    isUsernameAvailable = false; // Nome de usuário não disponível
                                }
                            }
                        } else {
                            textUsernameError.setText(getString(R.string.username_available));
                            textUsernameError.setTextColor(Color.parseColor("#006400"));
                            textUsernameError.setVisibility(View.VISIBLE);
                            isUsernameAvailable = true;  // Nome de usuário disponível
                        }
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Erro ao verificar a disponibilidade do nome de usuário", Toast.LENGTH_SHORT).show();
                        isUsernameAvailable = false; // Se houver erro, consideramos que não está disponível
                    }
                });
    }

    private void saveChanges() {
        String name = editNameTop.getText().toString().trim();
        String username = editUsername.getText().toString().trim();
        String bio = editBio.getText().toString().trim();
        String birthday = editBirthday.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            editNameTop.setError(getString(R.string.name_required_error));
            return;
        }

        if (TextUtils.isEmpty(username)) {
            editUsername.setError(getString(R.string.username_required_error));
            return;
        }

        if (!isUsernameAvailable) {
            // Se o nome de usuário não estiver disponível, impedir o salvamento
            Toast.makeText(EditProfileActivity.this, getString(R.string.username_not_available), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isUsernameValidFormat(username)) {
            textUsernameError.setText(getString(R.string.error_invalid_username));
            textUsernameError.setTextColor(Color.parseColor("#FF0000"));
            textUsernameError.setVisibility(View.VISIBLE);
            return;
        }

        if (tempSelectedPhotoUri != null) {
            // Fazer o upload da foto temporária para o Firebase Storage
            compressImageWithFFmpeg(tempSelectedPhotoUri);
        }

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("username", username);
        userMap.put("bio", bio);
        userMap.put("birthday", birthday);

        db.collection("users").document(currentUserId)
                .update(userMap)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(EditProfileActivity.this, getString(R.string.profile_updated_success), Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditProfileActivity.this, "Erro ao atualizar o perfil: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
