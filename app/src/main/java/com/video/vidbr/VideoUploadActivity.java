package com.video.vidbr;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.CameraManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.video.vidbr.adapter.HashtagSuggestionAdapter;
import com.video.vidbr.databinding.ActivityVideoUploadBinding;
import com.video.vidbr.model.VideoModel;
import com.video.vidbr.util.UiUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bumptech.glide.Glide;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class VideoUploadActivity extends AppCompatActivity {

    private ActivityVideoUploadBinding binding;
    private Uri selectedVideoUri = null;
    private ActivityResultLauncher<Intent> videoLauncher;
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageView close, capture, flipCamera;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;
    private EditText editDesc;

    // New fields for hashtag suggestions
    private RecyclerView suggestionRecyclerView;
    private HashtagSuggestionAdapter suggestionAdapter;
    private List<String> suggestionList = new ArrayList<>();

    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraFacing);
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoUploadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(R.drawable.back);
            actionBar.setTitle("");
        }

        VideoPlayerManager.getInstance().pauseVideo();

        suggestionRecyclerView = findViewById(R.id.suggestion_recycler_view);
        suggestionRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionAdapter = new HashtagSuggestionAdapter(suggestionList, this::onHashtagClick);
        suggestionRecyclerView.setAdapter(suggestionAdapter);

        editDesc = findViewById(R.id.post_caption_input);
        TextView textCharCount = findViewById(R.id.text_char_count);

        editDesc.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().contains("#")) {
                    String hashtagQuery = s.toString().substring(s.toString().lastIndexOf("#") + 1);
                    fetchHashtagSuggestions(hashtagQuery);
                } else {
                    // Hide suggestions if there's no hashtag
                    suggestionList.clear();
                    suggestionAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int remainingChars = editable.length();
                textCharCount.setText(remainingChars + "/1000");
            }
        });

        binding.friendsBtn.setOnClickListener(view -> {
            String currentText = binding.postCaptionInput.getText().toString();
            int cursorPosition = binding.postCaptionInput.getSelectionStart();

            if (cursorPosition == 0 || currentText.charAt(cursorPosition - 1) != '@') {
                binding.postCaptionInput.setText(currentText.substring(0, cursorPosition) + "@" + currentText.substring(cursorPosition));
            } else {
                binding.postCaptionInput.setText(currentText.substring(0, cursorPosition) + "@" + " " + currentText.substring(cursorPosition));
            }

            binding.postCaptionInput.setSelection(cursorPosition + 1);
        });

        binding.hashtagsBtn.setOnClickListener(view -> {
            String currentText = binding.postCaptionInput.getText().toString();
            int cursorPosition = binding.postCaptionInput.getSelectionStart();

            if (cursorPosition == 0 || currentText.charAt(cursorPosition - 1) != '#') {
                binding.postCaptionInput.setText(currentText.substring(0, cursorPosition) + "#" + currentText.substring(cursorPosition));
            } else {
                binding.postCaptionInput.setText(currentText.substring(0, cursorPosition) + "#" + " " + currentText.substring(cursorPosition));
            }

            binding.postCaptionInput.setSelection(cursorPosition + 1);
        });

        close = findViewById(R.id.close);
        previewView = findViewById(R.id.viewFinder);
        capture = findViewById(R.id.capture);
        flipCamera = findViewById(R.id.flipCamera);

        Spinner visibilitySpinner = findViewById(R.id.visibility_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.visibility_display_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        visibilitySpinner.setAdapter(adapter);

        int publicPosition = adapter.getPosition("Público");
        visibilitySpinner.setSelection(publicPosition);

        Button publishButton = findViewById(R.id.publish_button);
        publishButton.setOnClickListener(v -> {
            showPostView(); // Chama o método que já exibe o vídeo na post_view
        });

        Button closeIcon = findViewById(R.id.close_icon);
        closeIcon.setOnClickListener(v -> {
            // Verifica se o vídeo foi gravado
            if (selectedVideoUri != null) {
                // Excluir o vídeo gravado do armazenamento local, se necessário
                deleteVideoFromLocalStorage(selectedVideoUri);

                // Limpar a interface de usuário
                selectedVideoUri = null;  // Limpar a referência ao URI do vídeo gravado
                binding.postView.setVisibility(View.GONE);  // Ocultar a visualização do post
                binding.uploadView.setVisibility(View.VISIBLE);  // Exibir a tela de upload
                findViewById(R.id.add_icon).setVisibility(View.VISIBLE);  // Exibir ícone de adicionar
                findViewById(R.id.publish_button).setVisibility(View.GONE);  // Esconder botão de publicar
                findViewById(R.id.close_icon).setVisibility(View.GONE);  // Esconder ícone de fechar
            }
        });


        close.setOnClickListener(view -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        flipCamera.setOnClickListener(view -> {
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraFacing);
        });

        capture.setOnClickListener(view -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.CAMERA);
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.RECORD_AUDIO);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activityResultLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                captureVideo();
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(cameraFacing);
        }

        service = Executors.newSingleThreadExecutor();

        videoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                selectedVideoUri = result.getData().getData();
                showPostView();
            }
        });

        checkCameraAndAudioPermissions();
        binding.addIcon.setOnClickListener(v -> {
            String readExternalVideo = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? Manifest.permission.READ_MEDIA_VIDEO
                    : Manifest.permission.READ_EXTERNAL_STORAGE;

            if (ContextCompat.checkSelfPermission(this, readExternalVideo) == PackageManager.PERMISSION_GRANTED) {
                openVideoPicker();
            } else {
                checkReadWritePermissions();
            }
        });

        binding.submitPostBtn.setOnClickListener(v -> {
            try {
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    postVideo();
                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), getString(R.string.snackbar_message), Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        //binding.cancelPostBtn.setOnClickListener(v -> finish());
    }

    // Método para deletar o vídeo do armazenamento local
    private void deleteVideoFromLocalStorage(Uri videoUri) {
        if (videoUri != null) {
            String videoPath = getRealPathFromURI(videoUri);
            if (videoPath != null) {
                File videoFile = new File(videoPath);
                if (videoFile.exists()) {
                    boolean deleted = videoFile.delete();  // Excluir arquivo
                    if (!deleted) {
                        Toast.makeText(this, "Falha ao excluir o vídeo do armazenamento local.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void fetchHashtagSuggestions(String query) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("videos")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Usando um HashSet para garantir que as hashtags não sejam duplicadas
                        Set<String> uniqueHashtags = new HashSet<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            List<String> hashtags = (List<String>) document.get("hashtags");
                            if (hashtags != null) {
                                for (String hashtag : hashtags) {
                                    // Adiciona a hashtag no conjunto se começar com a query
                                    if (hashtag.toLowerCase().startsWith(query.toLowerCase())) {
                                        uniqueHashtags.add(hashtag);
                                    }
                                }
                            }
                        }

                        // Converte o HashSet de volta para uma lista e atualiza a sugestão
                        suggestionList.clear();
                        suggestionList.addAll(uniqueHashtags);

                        suggestionAdapter.notifyDataSetChanged();
                        suggestionRecyclerView.setVisibility(suggestionList.isEmpty() ? View.GONE : View.VISIBLE);
                    } else {
                        Toast.makeText(VideoUploadActivity.this, "Error fetching hashtags", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onHashtagClick(String hashtag) {
        String currentText = binding.postCaptionInput.getText().toString();
        String newText = currentText.substring(0, currentText.lastIndexOf("#") + 1) + hashtag + " ";
        binding.postCaptionInput.setText(newText);
        binding.postCaptionInput.setSelection(newText.length());
        suggestionList.clear();
        suggestionAdapter.notifyDataSetChanged();
        suggestionRecyclerView.setVisibility(View.GONE);
    }

    private void checkVideoForNSFW(Uri videoUri, Runnable onSuccess, Runnable onFailure) {
        try {
            // Atualizar UI para mostrar que a análise começou
            runOnUiThread(() -> {
                binding.submitPostBtn.setText(getString(R.string.analisando));
                binding.submitPostBtn.setClickable(false);
                binding.submitPostBtn.setFocusable(false);
            });

            File framesDir = new File(getCacheDir(), "nsfw_frames");
            if (!framesDir.exists()) {
                framesDir.mkdirs();
            }

            String inputPath = getRealPathFromURI(videoUri);
            String outputPattern = new File(framesDir, "frame_%02d.png").getAbsolutePath();

            String[] ffmpegCommand = {
                    "-i", inputPath,
                    "-vf", "fps=1/5",
                    "-vframes", "5",
                    outputPattern
            };

            FFmpeg.executeAsync(ffmpegCommand, (executionId, returnCode) -> {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    boolean isNSFW = false;
                    String explicitLabel = null;
                    NSFWDetector detector;
                    try {
                        detector = new NSFWDetector(getAssets());

                        for (int i = 1; i <= 5; i++) {
                            String framePath = new File(framesDir, String.format("frame_%02d.png", i)).getAbsolutePath();
                            File frameFile = new File(framePath);

                            if (frameFile.exists()) {
                                Bitmap bitmap = BitmapFactory.decodeFile(framePath);
                                if (bitmap != null) {
                                    float[] result = detector.detectNSFW(bitmap);
                                    float hentaiScore = result[1];
                                    float pornScore = result[3];

                                    if (hentaiScore >= 0.5f || pornScore >= 0.5f) {
                                        isNSFW = true;
                                        explicitLabel = hentaiScore > pornScore
                                                ? getString(R.string.label_hentai)
                                                : getString(R.string.label_porn);

                                        break;
                                    }
                                }
                            }
                        }

                        // Clean up temporary files
                        for (File file : framesDir.listFiles()) {
                            file.delete();
                        }

                        if (isNSFW) {
                            String finalExplicitLabel = explicitLabel;
                            runOnUiThread(() -> {
                                NSFWAlertBottomSheet bottomSheet = NSFWAlertBottomSheet.newInstance(finalExplicitLabel);
                                bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
                                binding.submitPostBtn.setText(getString(R.string.submit_button_text));
                                binding.submitPostBtn.setClickable(true);
                                binding.submitPostBtn.setFocusable(true);
                                onFailure.run();
                            });
                        } else {
                            runOnUiThread(() -> {
                                binding.submitPostBtn.setText(getString(R.string.processando));
                                onSuccess.run();
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> {
                            binding.submitPostBtn.setText(getString(R.string.submit_button_text));
                            binding.submitPostBtn.setClickable(true);
                            binding.submitPostBtn.setFocusable(true);
                            onFailure.run();
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        binding.submitPostBtn.setText(getString(R.string.submit_button_text));
                        binding.submitPostBtn.setClickable(true);
                        binding.submitPostBtn.setFocusable(true);
                        onFailure.run();
                    });
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                binding.submitPostBtn.setText(getString(R.string.submit_button_text));
                binding.submitPostBtn.setClickable(true);
                binding.submitPostBtn.setFocusable(true);
                onFailure.run();
            });
        }
    }

    private void postVideo() throws IOException {
        // Desabilitar o botão imediatamente ao clicar
        binding.submitPostBtn.setClickable(false);
        binding.submitPostBtn.setFocusable(false);

        if (binding.postCaptionInput.getText().toString().isEmpty()) {
            binding.postCaptionInput.setError(getString(R.string.error_empty_caption));
            binding.submitPostBtn.setClickable(true);
            binding.submitPostBtn.setFocusable(true);
            return;
        }

        if (selectedVideoUri != null) {
            long videoDuration = getVideoDuration(selectedVideoUri);
            if (videoDuration > 30) {
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.video_exceeds_limit), Snackbar.LENGTH_LONG).show();
                binding.submitPostBtn.setClickable(true);
                binding.submitPostBtn.setFocusable(true);
                binding.submitPostBtn.setText(getString(R.string.submit_button_text)); // ADICIONE ESTA LINHA
                return;
            }

            // Atualizar texto do botão para "Analisando..."
            binding.submitPostBtn.setText(getString(R.string.analisando));

            // Verificar conteúdo NSFW antes de continuar
            checkVideoForNSFW(selectedVideoUri, () -> {
                // Código para quando o vídeo é seguro (continua o processamento)
                setInProgress(true);
                binding.submitPostBtn.setText(getString(R.string.processando));

                String inputVideoPath = getRealPathFromURI(selectedVideoUri);
                if (inputVideoPath == null) {
                    Toast.makeText(this, "Failed to get video path", Toast.LENGTH_SHORT).show();
                    setInProgress(false);
                    binding.submitPostBtn.setClickable(true);
                    binding.submitPostBtn.setFocusable(true);
                    binding.submitPostBtn.setText(getString(R.string.submit_button_text));
                    return;
                }

                Date da = new Date();
                long ml = da.getTime();
                String outputVideoPath = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), ml + ".mp4").getAbsolutePath();

                String[] ffmpegCommand = {
                        "-i", inputVideoPath,
                        "-vf", "scale=576:1024",
                        "-crf", "40",
                        "-c:v", "libx264",
                        "-preset", "medium",
                        "-c:a", "aac",
                        "-b:v", "128k",
                        "-b:a", "64k",
                        outputVideoPath
                };

                FFmpeg.executeAsync(ffmpegCommand, new ExecuteCallback() {
                    @Override
                    public void apply(final long executionId, final int returnCode) {
                        if (returnCode == RETURN_CODE_SUCCESS) {
                            uploadVideoToBunnyCDN(Uri.fromFile(new File(outputVideoPath)), StorageConfig.STORAGE_ZONE, StorageConfig.API_KEY);
                        } else {
                            setInProgress(false);
                            Toast.makeText(VideoUploadActivity.this, "Video processing failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }, () -> {
                binding.submitPostBtn.setClickable(true);
                binding.submitPostBtn.setFocusable(true);
                binding.submitPostBtn.setText(getString(R.string.submit_button_text));
            });
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                return cursor.getString(columnIndex);
            }
        }
        return null; // Return null if the path can't be found
    }

    private long getVideoDuration(Uri videoUri) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(this, videoUri);
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Long.parseLong(durationStr) / 1000 : 0; // duração em segundos
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } finally {
            retriever.release();
        }
    }

    public void captureVideo() {
        capture.setImageResource(R.drawable.stop);
        Recording recording1 = recording;

        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return;
        }

        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/VidBr-Camera");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Exibir a ProgressBar
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        recording = videoCapture.getOutput().prepareRecording(this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);
                Handler handler = new Handler(Looper.getMainLooper());
                // Atualizar a ProgressBar a cada segundo
                handler.postDelayed(new Runnable() {
                    int elapsedTime = 0;
                    @Override
                    public void run() {
                        if (recording != null && elapsedTime < 30) {
                            elapsedTime++;
                            progressBar.setProgress(elapsedTime);
                            handler.postDelayed(this, 1000); // Atualiza a cada segundo
                        } else if (recording != null) {
                            recording.stop();
                            recording = null; // Limpar referência de gravação
                        }
                    }
                }, 1000);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                // Parar a ProgressBar
                progressBar.setVisibility(View.GONE);
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    Uri videoUri = ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    selectedVideoUri = videoUri; // Armazenar URI do vídeo gravado

                    findViewById(R.id.add_icon).setVisibility(View.GONE);
                    findViewById(R.id.publish_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.close_icon).setVisibility(View.VISIBLE);
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Erro: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                capture.setImageResource(R.drawable.record);
            }
        });
    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(this);

        processCameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = processCameraProvider.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.shutdown();
    }

    private void uploadVideoToBunnyCDN(Uri videoUri, String storageZone, String apiKey) {
        setInProgress(true); // Indica que o upload está em andamento

        // Converte o URI para o caminho absoluto do arquivo
        String videoPath = videoUri.getPath();

        if (videoPath != null) {
            File videoFile = new File(videoPath);

            if (videoFile.exists()) {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        // Construir a URL para o BunnyCDN
                        String urlStr = "https://storage.bunnycdn.com/" + storageZone + "/videos/" + videoFile.getName();
                        URL url = new URL(urlStr);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("PUT");
                        connection.setRequestProperty("AccessKey", apiKey);
                        connection.setRequestProperty("Content-Type", "application/octet-stream");
                        connection.setDoOutput(true);

                        // Fazer upload do vídeo
                        try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(videoFile));
                             BufferedOutputStream outputStream = new BufferedOutputStream(connection.getOutputStream())) {

                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }

                        // Verificar a resposta
                        int responseCode = connection.getResponseCode();
                        String responseMsg = connection.getResponseMessage();

                        Log.d("AuthTest", "Response Code: " + responseCode + " Response Message: " + responseMsg);

                        if (responseCode == HttpURLConnection.HTTP_CREATED) {
                            // Sucesso - obter a URL do vídeo ou outras informações
                            String videoUrl = "https://my-videos-2.b-cdn.net/videos/" + videoFile.getName();
                            runOnUiThread(() -> postToFirestore(videoUrl)); // Enviar a URL para o Firestore
                        } else {
                            runOnUiThread(() -> {
                                setInProgress(false);
                                Log.e("UploadError", "Falha no upload: " + responseCode + " " + responseMsg);
                            });
                        }
                    } catch (IOException e) {
                        runOnUiThread(() -> {
                            setInProgress(false);
                            Log.e("UploadError", "Falha no upload: " + e.getMessage());
                        });
                        Log.e("UploadError", "Exception: " + e.getMessage(), e);
                    }
                });
            } else {
                Log.e("FileError", "Arquivo de vídeo não encontrado!"); // Log de erro caso o arquivo não exista
                setInProgress(false);
            }
        } else {
            Log.e("UriError", "Caminho do vídeo inválido!"); // Log de erro caso o caminho seja nulo
            setInProgress(false);
        }
    }

    private void postToFirestore(String url) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String videoId = uid + "_" + System.currentTimeMillis();
        VideoModel videoModel = new VideoModel();
        videoModel.setVideoId(videoId);
        videoModel.setTitle(binding.postCaptionInput.getText().toString());
        videoModel.setUrl(url);
        videoModel.setUploaderId(uid);
        videoModel.setCreatedTime(Timestamp.now());

        List<String> hashtags = extractHashtags(binding.postCaptionInput.getText().toString());
        videoModel.setHashtags(hashtags);

        // Adicionar o país ao VideoModel
        Locale locale = Locale.getDefault();
        String countryInEnglish = locale.getDisplayCountry(Locale.ENGLISH);
        videoModel.setCountry(countryInEnglish);

        // Inicialize o campo likedBy como uma lista vazia
        videoModel.setLikedBy(new ArrayList<>());

        // Get visibility from spinner
        Spinner visibilitySpinner = findViewById(R.id.visibility_spinner);
        String visibility = (String) visibilitySpinner.getSelectedItem();
        String[] visibilityOptions = getResources().getStringArray(R.array.visibility_display_names);
        if (visibility.equals(visibilityOptions[0])) {
            videoModel.setVisibility("public");
        } else {
            videoModel.setVisibility("private");
        }

        FirebaseFirestore.getInstance().collection("videos")
                .document(videoModel.getVideoId())
                .set(videoModel)
                .addOnSuccessListener(aVoid -> {
                    setInProgress(false);
                })
                .addOnFailureListener(e -> {
                    setInProgress(false);
                    UiUtil.showToast(getApplicationContext(), "Video failed to upload");
                });

        Intent intent = new Intent(VideoUploadActivity.this, MainActivity.class);
        startActivity(intent);
		finish();
    }

    private List<String> extractHashtags(String caption) {
        List<String> hashtags = new ArrayList<>();
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(caption);
        while (matcher.find()) {
            hashtags.add(matcher.group(1)); // Adiciona apenas o grupo correspondente ao texto da hashtag sem o #
        }
        return hashtags;
    }

    private void setInProgress(boolean inProgress) {
        if (inProgress) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.submitPostBtn.setVisibility(View.GONE);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.submitPostBtn.setVisibility(View.VISIBLE);
        }
    }

    private void showPostView() {
        if (selectedVideoUri != null) {
            binding.postView.setVisibility(View.VISIBLE);
            binding.uploadView.setVisibility(View.GONE);
            findViewById(R.id.publish_button).setVisibility(View.GONE);
            findViewById(R.id.close_icon).setVisibility(View.GONE);
            Glide.with(binding.postThumbnailView)
                    .load(selectedVideoUri)
                    .apply(RequestOptions.bitmapTransform(new RoundedCorners(16)))
                    .into(binding.postThumbnailView);
        }
    }

    private void showPermissionAlertDialog(String message, Runnable onPositiveClick, Runnable onNegativeClick) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);

        TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
        dialogMessage.setText(message);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        Button positiveButton = dialogView.findViewById(R.id.dialog_positive_button);
        positiveButton.setOnClickListener(v -> {
            if (onPositiveClick != null) {
                onPositiveClick.run();
            }
            dialog.dismiss();
        });

        Button negativeButton = dialogView.findViewById(R.id.dialog_negative_button);
        negativeButton.setOnClickListener(v -> {
            if (onNegativeClick != null) {
                onNegativeClick.run();  // Aqui você pode adicionar a lógica de fechar a atividade ou o diálogo
            }
            dialog.dismiss();  // Fechar o AlertDialog
        });

        dialog.getWindow().setBackgroundDrawableResource(R.drawable.alert_dialog_background);

        dialog.show();
    }

    private void checkReadWritePermissions() {
        String readExternalVideo;
        String writeInternalStorage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalVideo = Manifest.permission.READ_MEDIA_VIDEO;
            writeInternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        } else {
            readExternalVideo = Manifest.permission.READ_EXTERNAL_STORAGE;
            writeInternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }

        // Verifica se as permissões de leitura e escrita foram concedidas
        if (ContextCompat.checkSelfPermission(this, readExternalVideo) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, writeInternalStorage) != PackageManager.PERMISSION_GRANTED) {

            // Exibe um AlertDialog para pedir permissões de leitura e escrita
            showPermissionAlertDialog(
                    getString(R.string.permission_request_message),
                    this::requestReadWritePermissions,
                    () -> {
                        // Lógica do botão negativo
                    }
            );
        }
    }

    private void requestReadWritePermissions() {
        String readExternalVideo;
        String writeInternalStorage;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            readExternalVideo = Manifest.permission.READ_MEDIA_VIDEO;
            writeInternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        } else {
            readExternalVideo = Manifest.permission.READ_EXTERNAL_STORAGE;
            writeInternalStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        }

        // Solicita as permissões de leitura e escrita
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        readExternalVideo,
                        writeInternalStorage
                },
                101 // Novo código de solicitação
        );
    }

    private void checkCameraAndAudioPermissions() {
        // Verifica se as permissões de câmera e áudio foram concedidas
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Exibe um AlertDialog para pedir permissões de câmera e áudio
            showPermissionAlertDialog(
                    getString(R.string.permission_request_camera_audio),
                    this::requestCameraAndAudioPermissions,
                    () -> {
                        Intent intent = new Intent(this, MainActivity.class);
                        startActivity(intent);
                        overridePendingTransition(0, 0);
                        finish();
                    }
            );
        }
    }

    private void requestCameraAndAudioPermissions() {
        // Solicita as permissões de câmera e áudio
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO
                },
                102 // Novo código de solicitação
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Verifica se alguma permissão foi negada
        boolean allPermissionsGranted = true;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_DENIED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (!allPermissionsGranted) {
            // Identifica qual conjunto de permissões foi negado com base no requestCode
            if (requestCode == 101) { // Permissões de leitura/escrita
                showPermissionAlertDialog(
                        getString(R.string.permission_request_message),
                        this::requestReadWritePermissions,
                        () -> {
                            // Lógica do botão negativo
                        }
                );

            } else if (requestCode == 102) { // Permissões de câmera/áudio
                showPermissionAlertDialog(
                        getString(R.string.permission_request_camera_audio),
                        this::requestCameraAndAudioPermissions,
                        () -> {
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                            overridePendingTransition(0, 0);
                            finish();
                        }
                );
            }
        }
    }

    private void openVideoPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        intent.setType("video/*");
        videoLauncher.launch(intent);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }
}
