package com.video.vidbr;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.databinding.ActivityProfileBinding;
import com.video.vidbr.fragments.LikedVideosFragment;
import com.video.vidbr.fragments.UserVideosFragment;
import com.video.vidbr.fragments.VisibilityVideosFragment;

import com.video.vidbr.model.UserModel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {
    private ActivityProfileBinding binding;
    private String profileUserId;
    private String currentUserId;
    private ActivityResultLauncher<Intent> photoLauncher;
    private UserModel profileUserModel;

    private static final String PREFS_NAME = "ProfilePrefs";
    private static final String KEY_LAST_AD_TIME = "last_ad_time";
    private static final long ONE_HOUR_MILLIS = 1 * 60 * 60 * 1000; // 1 hora

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        profileUserId = getIntent().getStringExtra("profile_user_id");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        // Initialize the ActivityResultLauncher for picking photos
        photoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri photoUri = result.getData().getData();
                if (photoUri != null) {
                    compressImageWithFFmpeg(photoUri);
                }
            }
        });

        setupTabLayout();
        VideoPlayerManager.getInstance().pauseVideo();

        binding.profilePic.setOnClickListener(v -> showProfilePictureDialog());

        if (currentUserId != null && profileUserId.equals(currentUserId)) {
            binding.profileBtn.setText(R.string.logout_label);
            binding.profileBtn.setOnClickListener(v -> logout());
            binding.messageBtn.setVisibility(View.GONE);
        } else {
            binding.profileBtn.setText(R.string.follow);
            binding.profileBtn.setOnClickListener(v -> followUnfollowUser());
        }

        binding.followerCount.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, FollowingActivity.class);
            intent.putExtra("current_user_id", profileUserModel.getId());
            intent.putExtra("current_user_name", profileUserModel.getUsername());
            intent.putExtra("tab_to_select", 1); // Pass 1 to select "Seguidores" tab
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.followingCount.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, FollowingActivity.class);
            intent.putExtra("current_user_id", profileUserModel.getId());
            intent.putExtra("current_user_name", profileUserModel.getUsername());
            intent.putExtra("tab_to_select", 0); // Pass 0 to select "Seguindo" tab
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.messageBtn.setOnClickListener(view -> {
            Intent intent = new Intent(ProfileActivity.this, ChatActivity.class);
            intent.putExtra("chatUserId", profileUserModel.getId());
            intent.putExtra("chatUserName", profileUserModel.getUsername());
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        binding.editPro.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        binding.bottomNavBar.setItemIconTintList(null);

        Menu menu = binding.bottomNavBar.getMenu();
        MenuItem profileItem = menu.findItem(R.id.bottom_menu_profile);

        profileItem.setIcon(R.drawable.icon_profile_black);

        binding.bottomNavBar.setItemIconTintList(null);
        binding.bottomNavBar.setOnItemSelectedListener(menuItem -> {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.bottom_menu_home) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("profile_user_id", FirebaseAuth.getInstance().getCurrentUser() != null ?
                        FirebaseAuth.getInstance().getCurrentUser().getUid() : null);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
            }
            if (itemId == R.id.bottom_menu_add_video) {
                startActivity(new Intent(this, VideoUploadActivity.class));
                overridePendingTransition(0, 0);
            }
            if (itemId == R.id.bottom_menu_profile) {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    Intent intent = new Intent(ProfileActivity.this, ProfileActivity.class);
                    intent.putExtra("profile_user_id", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                } else {
                    VideoPlayerManager.getInstance().pauseVideo();
                    startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                }
            }
            if (itemId == R.id.bottom_menu_search) {
                // Handle chat action
                startActivity(new Intent(ProfileActivity.this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
            if (itemId == R.id.bottom_menu_chat) {
                // Handle chat action
                startActivity(new Intent(ProfileActivity.this, UsersWhoSentMessagesActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }

            return false;
        });

        findViewById(R.id.menu_icon).setOnClickListener(v -> {
            BottomSheetMenuFragment bottomSheet = new BottomSheetMenuFragment();
            bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
        });

        getProfileDataFromFirebase();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long lastAdTime = prefs.getLong(KEY_LAST_AD_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if ((currentTime - lastAdTime) >= ONE_HOUR_MILLIS ) {
            AdRequest adRequest = new AdRequest.Builder().build();
            InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                    new InterstitialAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                            interstitialAd.show(ProfileActivity.this);

                            // Salva o novo timestamp após exibir o anúncio
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putLong(KEY_LAST_AD_TIME, currentTime);
                            editor.apply();
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Falha ao carregar o anúncio, continue normalmente
                        }
                    });
        }
    }

    private void compressImageWithFFmpeg(Uri photoUri) {
        String inputPath = getRealPathFromURI(photoUri);
        if (inputPath == null) {
            return;
        }

        // Primeiro verifica se a imagem é NSFW
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            NSFWDetector detector = new NSFWDetector(getAssets());
            float[] result = detector.detectNSFW(bitmap);

            // Verificar apenas Hentai (1) e Porn (3)
            float hentaiScore = result[1];
            float pornScore = result[3];

            if (hentaiScore >= 0.5f || pornScore >= 0.5f) {
                String explicitLabel = getString(hentaiScore > pornScore ? R.string.label_hentai : R.string.label_porn);
                NSFWAlertBottomSheet bottomSheet = NSFWAlertBottomSheet.newInstance(explicitLabel);
                bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
                return; // Não prossegue com o upload
            }

            // Se passar na verificação, continua com a compressão e upload
            File compressedImageFile = new File(getCacheDir(), "compressed_image.webp");
            String outputPath = compressedImageFile.getAbsolutePath();

            String[] command = {
                    "-y",
                    "-i", inputPath,
                    "-vf", "scale=640:-1",
                    "-c:v", "libwebp",
                    "-q:v", "50",
                    "-preset", "picture",
                    "-map_metadata", "-1",
                    outputPath
            };

            FFmpeg.executeAsync(command, (executionId, returnCode) -> {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    Uri compressedImageUri = Uri.fromFile(compressedImageFile);
                    uploadToBunnyCDN(compressedImageUri, compressedImageFile, StorageConfig.STORAGE_ZONE, StorageConfig.API_KEY);
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erro ao verificar a imagem.", Toast.LENGTH_SHORT).show();
        }
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


    private void setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.grid));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.like_black));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setIcon(R.drawable.ic_lock));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        showUserVideosFragment();
                        break;
                    case 1:
                        showLikedVideosFragment();
                        break;
                    case 2:
                        showVisibilityVideosFragment();
                        break;
                    default:
                        // Handle unexpected tab position
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No action needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // No action needed
            }
        });

        // Initially show the first tab (assuming 0 is the default index)
        showUserVideosFragment();
    }

    private void showUserVideosFragment() {
        UserVideosFragment userVideosFragment = new UserVideosFragment();
        Bundle args = new Bundle();
        args.putString("profile_user_id", profileUserId);
        userVideosFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, userVideosFragment)
                .commit();
    }

    private void showLikedVideosFragment() {
        LikedVideosFragment likedVideosFragment = new LikedVideosFragment();
        Bundle args = new Bundle();
        args.putString("profile_user_id", profileUserId);
        likedVideosFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, likedVideosFragment)
                .commit();
    }

    private void showVisibilityVideosFragment() {
        VisibilityVideosFragment visibilityVideosFragment = new VisibilityVideosFragment();
        Bundle args = new Bundle();
        args.putString("profile_user_id", profileUserId);
        visibilityVideosFragment.setArguments(args);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, visibilityVideosFragment)
                .commit();
    }

    private void followUnfollowUser() {
        if (currentUserId == null) {
            // Se o usuário não estiver logado, redireciona para a tela de login
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel currentUserModel = documentSnapshot.toObject(UserModel.class);

                    if (profileUserModel.getFollowerList().contains(currentUserId)) {
                        profileUserModel.getFollowerList().remove(currentUserId);
                        currentUserModel.getFollowingList().remove(profileUserId);

                        binding.profileBtn.setText(R.string.follow);
                        binding.messageBtn.setVisibility(View.GONE);

                    } else {
                        profileUserModel.getFollowerList().add(currentUserId);
                        currentUserModel.getFollowingList().add(profileUserId);

                        binding.profileBtn.setText(R.string.unfollow);
                        binding.messageBtn.setVisibility(View.VISIBLE);
                    }

                    // Atualizar os dados do perfil e do usuário atual no Firestore
                    updateUserData(profileUserModel);
                    updateUserData(currentUserModel);
                });
    }


    private void updateUserData(UserModel model) {
        FirebaseFirestore.getInstance().collection("users")
                .document(model.getId())
                .set(model)
                .addOnSuccessListener(aVoid -> getProfileDataFromFirebase());
    }

    private void uploadToBunnyCDN(Uri photoUri, File compressedImageFile, String storageZone, String apiKey) {
        binding.progressBar.setVisibility(View.VISIBLE);

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
                                    // Call postToFirestore after updating Firestore
                                    postToFirestore(imageUrl); // Call your postToFirestore method here
                                });

                        // Deleta o arquivo comprimido local após o upload, se o arquivo já existir, excluí-lo
                        if (compressedImageFile.exists()) {
                            compressedImageFile.delete();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void postToFirestore(String url) {
        if (currentUserId == null) {
            // Handle case where user is not logged in
            return;
        }

        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .update("profilePic", url)
                .addOnSuccessListener(aVoid -> getProfileDataFromFirebase());
    }

    private void checkPermissionAndPickPhoto() {
        String readExternalPhoto;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalPhoto = android.Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            readExternalPhoto = android.Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        if (ContextCompat.checkSelfPermission(this, readExternalPhoto) == PackageManager.PERMISSION_GRANTED) {
            openPhotoPicker();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{readExternalPhoto}, 100);
        }
    }

    private void openPhotoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        photoLauncher.launch(intent);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void getProfileDataFromFirebase() {
        FirebaseFirestore.getInstance().collection("users")
                .document(profileUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    profileUserModel = documentSnapshot.toObject(UserModel.class);
                    setUI();
                });
    }

    private void setUI() {
        if (profileUserModel != null) {
            Glide.with(binding.profilePic)
                    .load(profileUserModel.getProfilePic())
                    .apply(new RequestOptions().placeholder(R.drawable.icon_account_circle)).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(binding.profilePic);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userRef = db.collection("users").document(profileUserId);

            userRef.addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) {
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    UserModel profileUserModel = documentSnapshot.toObject(UserModel.class);
                    if (profileUserModel != null) {
                        binding.userNameTop.setText(profileUserModel.getName());
                        binding.profileUsername.setText("@" + profileUserModel.getUsername());
                        binding.followingCount.setText(formatLikesCount(profileUserModel.getFollowingList().size()));
                        binding.followerCount.setText(formatLikesCount(profileUserModel.getFollowerList().size()));
                        binding.profileBio.setText(profileUserModel.getBio());
                    }
                }
            });

            binding.profileUsername.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String username = profileUserModel.getUsername();

                    ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("username", username);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(v.getContext(), v.getContext().getString(R.string.id_copiado), Toast.LENGTH_SHORT).show();
                }
            });

            // Enable link handling for the bio
            binding.profileBio.setMovementMethod(LinkMovementMethod.getInstance());

            if (currentUserId != null) {
                if (profileUserModel.getFollowerList().contains(currentUserId)) {
                    binding.profileBtn.setText(R.string.unfollow);
                    binding.messageBtn.setVisibility(View.VISIBLE);
                } else {
                    binding.messageBtn.setVisibility(View.GONE);
                }

                // Show "Editar Perfil" button if the current user is viewing their own profile
                if (profileUserId.equals(currentUserId)) {
                    binding.editPro.setVisibility(View.VISIBLE);
                } else {
                    binding.editPro.setVisibility(View.GONE);
                }
            } else {
                binding.profileBtn.setText(R.string.follow);
                binding.messageBtn.setVisibility(View.GONE);
                binding.editPro.setVisibility(View.GONE);
            }

            FirebaseFirestore.getInstance().collection("videos")
                    .whereEqualTo("uploaderId", profileUserId)
                    .whereEqualTo("visibility", "public")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots ->
                            binding.postCount.setText(formatLikesCount(queryDocumentSnapshots.size()))
                    );

            if (profileUserModel.isVerified()) {
                binding.verifiedIcon.setVisibility(View.VISIBLE);
            } else {
                binding.verifiedIcon.setVisibility(View.GONE);
            }

            if (profileUserModel.isVerifiedGold()) {
                binding.verifiedGold.setVisibility(View.VISIBLE);
            } else {
                binding.verifiedGold.setVisibility(View.GONE);
            }

            binding.progressBar.setVisibility(View.INVISIBLE);
        }

        if (currentUserId != null && profileUserId.equals(currentUserId)) {
            binding.menuIcon.setImageResource(R.drawable.menu);
        } else {
            binding.bottomNavBar.setVisibility(View.GONE);
            binding.menuIcon.setImageResource(R.drawable.share_profile);
            binding.menuIcon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
            binding.menuIcon.setOnClickListener(v -> shareProfileLink());
        }
    }

    private void shareProfileLink() {
        String profileLink = "https://www.vidbr.com.br/profile.html?username=" + profileUserModel.getUsername(); // Substitua com o link do perfil real

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_profile_message, profileUserModel.getName(), profileLink));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_profile_title)));
    }

    private void showProfilePictureDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_profile_picture, null);

        ImageView dialogProfilePic = dialogView.findViewById(R.id.dialog_profile_pic);
        Button buttonChangePicture = dialogView.findViewById(R.id.button_change_picture);
        ImageView iconExpand = dialogView.findViewById(R.id.icon_expand);
        ImageView iconDelete = dialogView.findViewById(R.id.icon_delete);

        Glide.with(this)
                .load(profileUserModel.getProfilePic())
                .apply(new RequestOptions().placeholder(R.drawable.icon_account_circle)).diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(dialogProfilePic);

        if (currentUserId != null && profileUserId.equals(currentUserId)) {
            buttonChangePicture.setVisibility(View.VISIBLE);
            iconDelete.setVisibility(View.VISIBLE);
            iconDelete.setOnClickListener(v -> {
                // Handle delete action here
                // e.g., prompt user to confirm deletion, then proceed with deletion logic
            });
        } else {
            buttonChangePicture.setVisibility(View.GONE);
            iconDelete.setVisibility(View.GONE);
        }

        iconDelete.setOnClickListener(v -> {
            AlertDialog.Builder confirmDeleteBuilder = new AlertDialog.Builder(this);
            confirmDeleteBuilder.setMessage(getString(R.string.confirm_delete_message))
                    .setPositiveButton(getString(R.string.yes_button), (dialog, which) -> {
                        FirebaseFirestore.getInstance().collection("users")
                                .document(currentUserId)
                                .update("profilePic", null)
                                .addOnSuccessListener(aVoid1 -> {
                                    // Update the UI accordingly
                                    profileUserModel.setProfilePic(null);
                                    setUI(); // Refresh UI to reflect changes
                                });

                        // Delete from BunnyCDN (add this logic)
                        String storageZone = StorageConfig.STORAGE_ZONE; // Replace with your BunnyCDN storage zone
                        String apiKey = StorageConfig.API_KEY; // Replace with your BunnyCDN API key
                        String oldFileName = Uri.parse(profileUserModel.getProfilePic()).getLastPathSegment();
                        if (oldFileName != null) {
                            deleteFromBunnyCDN(storageZone, apiKey, oldFileName);
                        }

                        Glide.with(this)
                                .load(R.drawable.icon_account_circle)  // Default profile icon
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .circleCrop()
                                .into(dialogProfilePic);
                    })
                    .setNegativeButton(getString(R.string.no_button), (dialog, which) -> dialog.dismiss())
                    .show();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        buttonChangePicture.setOnClickListener(v -> {
            alertDialog.dismiss();
            try {
                // Verifica se o detector pode ser carregado antes de pedir a imagem
                new NSFWDetector(getAssets()); // Testa a inicialização
                checkPermissionAndPickPhoto();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Erro ao inicializar verificador de conteúdo. Tente novamente.", Toast.LENGTH_SHORT).show();
            }
        });

        iconExpand.setOnClickListener(v -> {
            showExpandedImageDialog(profileUserModel.getProfilePic());
        });
    }

    private void deleteFromBunnyCDN(String storageZone, String apiKey, String fileName) {
        // Use ExecutorService to run the network operation in a background thread
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

    private void showExpandedImageDialog(String imageUrl) {
        LayoutInflater inflater = getLayoutInflater();
        View expandedImageView = inflater.inflate(R.layout.dialog_expanded_image, null);

        ImageView expandedImage = expandedImageView.findViewById(R.id.expanded_image);

        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions().placeholder(R.drawable.icon_account_circle)).diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(expandedImage);

        AlertDialog.Builder expandedImageBuilder = new AlertDialog.Builder(this, R.style.FullScreenDialogStyle);
        expandedImageBuilder.setView(expandedImageView);
        AlertDialog expandedImageDialog = expandedImageBuilder.create();
        expandedImageDialog.show();
    }

    private String formatLikesCount(int likesCount) {
        if (likesCount < 1000) {
            return String.valueOf(likesCount);
        } else if (likesCount < 1000000) {
            return String.format("%.1f mil", likesCount / 1000.0);
        } else if (likesCount < 1000000000) {
            return String.format("%.1f milhões", likesCount / 1000000.0);
        } else if (likesCount < 1000000000000L) {
            return String.format("%.1f bilhões", likesCount / 1000000000.0);
        } else {
            return String.format("%.1f trilhões", likesCount / 1000000000000.0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openPhotoPicker();
        }
    }
}
