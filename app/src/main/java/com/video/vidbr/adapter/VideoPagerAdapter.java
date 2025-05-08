package com.video.vidbr.adapter;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.video.vidbr.CacheDataSourceFactory;
import com.video.vidbr.HashtagVideosActivity;
import com.video.vidbr.LikedUsersActivity;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.StorageConfig;
import com.video.vidbr.ThumbnailBottomSheetFragment;
import com.video.vidbr.VideoPlayerManager;
import com.video.vidbr.databinding.ItemVideoPagerBinding;
import com.video.vidbr.fragments.BottomSheetLoginFragment;
import com.video.vidbr.model.CommentModel;
import com.video.vidbr.model.UserModel;
import com.video.vidbr.model.VideoModel;
import com.video.vidbr.util.TimeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoPagerAdapter extends RecyclerView.Adapter<VideoPagerAdapter.VideoViewHolder> {

    private List<Object> videoList; // List to store both VideoModel and AdView
    public VideoViewHolder currentViewHolder;
    private static final MutableLiveData<VideoModel> videoModelLiveData = new MutableLiveData<>();
    private static final int SHARE_REQUEST_CODE = 1001;

    public VideoPagerAdapter(List<Object> videoList) {
        this.videoList = videoList;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVideoPagerBinding binding = ItemVideoPagerBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new VideoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        if (position != RecyclerView.NO_POSITION && position < getItemCount()) {
            holder.bindVideo((VideoModel) videoList.get(position));
            currentViewHolder = holder;
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    @Override
    public void onViewRecycled(@NonNull VideoViewHolder holder) {
        super.onViewRecycled(holder);
        holder.releasePlayer();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder.player != null) {
            holder.player.setPlayWhenReady(true); // Start or resume playback
            holder.player.setRepeatMode(Player.REPEAT_MODE_ALL);
            holder.binding.pauseIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.player != null) {
            holder.player.setPlayWhenReady(false); // Pause playback
        }
    }

    public void releaseCurrentVideo() {
        if (currentViewHolder != null) {
            currentViewHolder.releasePlayer();
        }
    }

    // Método para pausar o vídeo
    public void pauseCurrentVideo() {
        if (currentViewHolder != null) {
            currentViewHolder.pauseVideo();
        }
    }

    public void removeVideo(VideoModel video) {
        int position = videoList.indexOf(video);
        if (position != -1) {
            videoList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {

        private ItemVideoPagerBinding binding;
        private SimpleExoPlayer player;

        public VideoViewHolder(ItemVideoPagerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            // Observe videoModelLiveData to update likes info
            videoModelLiveData.observeForever(this::updateLikesInfo);
        }


        private boolean isPlaying = false;

        private void togglePlayback() {
            if (binding.videoView.getPlayer() != null) {
                if (isPlaying) {
                    binding.videoView.getPlayer().setPlayWhenReady(false);
                    binding.pauseIcon.setVisibility(View.VISIBLE);
                    binding.loading.setVisibility(View.GONE);
                } else {
                    binding.videoView.getPlayer().setPlayWhenReady(true);
                    binding.pauseIcon.setVisibility(View.GONE);
                }
                isPlaying = !isPlaying;
            }
        }

        private void bindVideo(VideoModel video) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firestore.collection("users")
                    .document(video.getUploaderId())
                    .get().addOnSuccessListener(documentSnapshot -> {
                        UserModel userModel = documentSnapshot.toObject(UserModel.class);
                        if (userModel != null) {
                            binding.usernameView.setText(userModel.getName());

                            // Update visibility of verified icons
                            binding.verifiedIcon.setVisibility(userModel.isVerified() ? View.VISIBLE : View.GONE);
                            binding.verifiedGold.setVisibility(userModel.isVerifiedGold() ? View.VISIBLE : View.GONE);

                            Glide.with(binding.profileIcon.getContext())
                                    .asBitmap()
                                    .load(userModel.getProfilePic())
                                    .apply(new RequestOptions().circleCrop())
                                    .into(new BitmapImageViewTarget(binding.profileIcon) {
                                        @Override
                                        protected void setResource(Bitmap resource) {
                                            if (resource == null) {
                                                // Handle the null bitmap scenario
                                                binding.profileIcon.setImageResource(R.drawable.icon_account_circle); // Set a default image or handle appropriately
                                                return;
                                            }

                                            int borderWidth = 6; // Define the border width
                                            int borderColor = Color.WHITE; // Define the border color

                                            // Create a new bitmap with space for the border
                                            Bitmap borderedBitmap = Bitmap.createBitmap(resource.getWidth() + borderWidth * 2, resource.getHeight() + borderWidth * 2, resource.getConfig());
                                            Canvas canvas = new Canvas(borderedBitmap);

                                            // Draw the circle with the border
                                            Paint borderPaint = new Paint();
                                            borderPaint.setColor(borderColor);
                                            borderPaint.setStyle(Paint.Style.FILL);

                                            float radius = (Math.min(resource.getWidth(), resource.getHeight()) / 2f) + borderWidth;
                                            canvas.drawCircle(borderedBitmap.getWidth() / 2f, borderedBitmap.getHeight() / 2f, radius, borderPaint);

                                            // Draw the original image in the center of the circle
                                            canvas.drawBitmap(resource, borderWidth, borderWidth, null);

                                            // Create a RoundedBitmapDrawable from the bordered bitmap
                                            RoundedBitmapDrawable circularDrawable = RoundedBitmapDrawableFactory.create(binding.profileIcon.getResources(), borderedBitmap);
                                            circularDrawable.setCircular(true);
                                            binding.profileIcon.setImageDrawable(circularDrawable);
                                        }

                                        @Override
                                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                            // Handle the failure case
                                            binding.profileIcon.setImageResource(R.drawable.icon_account_circle); // Set a default image or handle appropriately
                                        }
                                    });

                            binding.profileIcon.setOnClickListener(view -> {
                                pauseVideo();
                                Intent intent = new Intent(binding.userDetailLayout.getContext(), ProfileActivity.class);
                                intent.putExtra("profile_user_id", userModel.getId());
                                binding.userDetailLayout.getContext().startActivity(intent);
                            });

                            binding.usernameView.setOnClickListener(view -> {
                                pauseVideo();
                                Intent intent = new Intent(binding.userDetailLayout.getContext(), ProfileActivity.class);
                                intent.putExtra("profile_user_id", userModel.getId());
                                binding.userDetailLayout.getContext().startActivity(intent);
                            });

                            if (currentUser != null && currentUser.getUid().equals(userModel.getId())) {
                                binding.more.setVisibility(View.VISIBLE);
                            } else {
                                binding.more.setVisibility(View.GONE);
                            }

                            // Verifica se os downloads estão ativados sem fazer outra consulta
                            Boolean isDownloadEnabled = documentSnapshot.getBoolean("downloadEnabled");
                            binding.download.setVisibility((isDownloadEnabled != null && isDownloadEnabled) ? View.VISIBLE : View.GONE);
                        }
                    });

            ImageView imageView = binding.loading.findViewById(R.id.loading);
            Glide.with(itemView.getContext()).asGif().load(R.drawable.loading).into(imageView);
            imageView.setVisibility(View.VISIBLE);

            Context context = binding.videoView.getContext();

            DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                    .setEnableDecoderFallback(true);

            player = new SimpleExoPlayer.Builder(context, renderersFactory).build();
            binding.videoView.setPlayer(player);

            DataSource.Factory cacheDataSourceFactory = new CacheDataSourceFactory(context);

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(video.getVideoUrl()));

            MediaSource mediaSource = new ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(mediaItem);

            player.setMediaSource(mediaSource);
            player.prepare();

            player.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) {
                        imageView.setVisibility(View.GONE);
                        binding.videoView.setUseController(true);
                        VideoPlayerManager.getInstance().setPlayer(player);
                    } else {
                        imageView.setVisibility(View.VISIBLE);
                    }
                }
            });

            setCaptionWithHashtags(video.getTitle());
            listenForCommentChanges(video);

            updateLikesInfo(video);
            binding.likeIcon.setOnClickListener(view -> onLikeClick(video));

            binding.share.setOnClickListener(view -> shareVideo(view.getContext(), video, binding.shareCount));
            updateShareCount(video.getId());

            binding.download.setOnClickListener(view -> {
                // Handle save action
                String videoUrl = video.getVideoUrl();
                if (videoUrl != null) {
                    downloadAndProcessVideo(view.getContext(), videoUrl);
                }
            });

            binding.bottomBar.setOnClickListener(view -> {
                // Abre a tela com a lista de usuários que curtiram o vídeo
                Intent intent = new Intent(view.getContext(), LikedUsersActivity.class);
                intent.putExtra("videoId", video.getVideoId());
                ((Activity) view.getContext()).startActivity(intent);
            });

            DefaultTimeBar timeBar = binding.videoView.findViewById(R.id.exo_progress);
            timeBar.addListener(new TimeBar.OnScrubListener() {
                @Override
                public void onScrubStart(TimeBar timeBar, long position) {}

                @Override
                public void onScrubMove(TimeBar timeBar, long position) {
                    updateProgressText(position);
                    binding.userDetailLayout.setVisibility(View.GONE);
                    binding.timeLayout.setVisibility(View.VISIBLE);
                }

                @Override
                public void onScrubStop(TimeBar timeBar, long position, boolean canceled) {
                    updateProgressText(position);
                    binding.userDetailLayout.setVisibility(View.VISIBLE);
                    binding.timeLayout.setVisibility(View.GONE);
                }
            });

            binding.commentNumber.setText(formatLikesCount(video.getCommentCount()));

            binding.videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

            Timestamp createdTime = video.getCreatedTime();
            String timeAgo = TimeUtils.getTimeAgo(createdTime, binding.videoView.getContext());

            binding.timeAgoView.setText(timeAgo);

            binding.comment.setOnClickListener(v -> {
                BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext(), R.style.ThemeOverlay_App_BottomSheetDialog);
                LayoutInflater inflater = LayoutInflater.from(v.getContext());
                View bottomSheetView = inflater.inflate(R.layout.activity_comments, null);
                bottomSheetDialog.setContentView(bottomSheetView);

                RecyclerView recyclerView = bottomSheetView.findViewById(R.id.recyclerView_comments);
                ProgressBar progressBar = bottomSheetView.findViewById(R.id.progress_bar);
                recyclerView.setLayoutManager(new LinearLayoutManager(v.getContext()));
                List<CommentModel> commentList = new ArrayList<>();
                CommentsAdapter adapter = new CommentsAdapter(commentList, video.getVideoId());
                recyclerView.setAdapter(adapter);

                progressBar.setVisibility(View.VISIBLE); // Show progress bar while loading comments

                FirebaseFirestore.getInstance().collection("videos")
                        .document(video.getId())
                        .collection("comments")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(10) // Load the first 10 comments initially
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            commentList.clear();
                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                CommentModel comment = document.toObject(CommentModel.class);
                                if (comment != null) {
                                    commentList.add(comment);
                                }
                            }
                            adapter.notifyDataSetChanged();
                            progressBar.setVisibility(View.GONE); // Hide progress bar when comments are loaded

                            TextView noCommentsTextView = bottomSheetView.findViewById(R.id.no_comments_text_view);
                            RelativeLayout.LayoutParams layoutParams =
                                    (RelativeLayout.LayoutParams) noCommentsTextView.getLayoutParams();
                            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
                            noCommentsTextView.setLayoutParams(layoutParams);

                            if (commentList.isEmpty()) {
                                noCommentsTextView.setVisibility(View.VISIBLE); // Show no comments message
                            } else {
                                noCommentsTextView.setVisibility(View.GONE); // Hide no comments message
                            }
                        })
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                            Toast.makeText(v.getContext(), "Failed to load comments", Toast.LENGTH_SHORT).show();
                        });

                // Load more comments on scroll
                recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (!recyclerView.canScrollVertically(1)) { // If scrolled to the bottom
                            if (!commentList.isEmpty()) { // Check if the list is not empty
                                // Load more comments
                                FirebaseFirestore.getInstance().collection("videos")
                                        .document(video.getId())
                                        .collection("comments")
                                        .orderBy("timestamp", Query.Direction.DESCENDING)
                                        .startAfter(commentList.get(commentList.size() - 1).getTimestamp()) // Start after the last loaded comment
                                        .limit(10)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            for (DocumentSnapshot document : queryDocumentSnapshots) {
                                                CommentModel comment = document.toObject(CommentModel.class);
                                                if (comment != null && !commentList.contains(comment)) {
                                                    commentList.add(comment);
                                                }
                                            }
                                            adapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(v.getContext(), "Failed to load more comments", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    }
                });

                EditText commentEditText = bottomSheetView.findViewById(R.id.editText_comment);

                // Emoji click listeners
                int[] emojiIds = {
                        R.id.emoji_laughing,
                        R.id.emoji_heart,
                        R.id.emoji_rolling,
                        R.id.emoji_heart_eyes,
                        R.id.emoji_pray,
                        R.id.emoji_pleading,
                        R.id.emoji_thinking,
                        R.id.emoji_sweat,
                        R.id.emoji_party,
                        R.id.emoji_hugging,
                        R.id.emoji_winking
                };

                String[] emojis = {
                        "😂", "❤️", "🤣", "😍", "🙏", "🥺", "🤔", "😅", "🎉", "🤗", "😜"
                };

                for (int i = 0; i < emojiIds.length; i++) {
                    int finalI = i;
                    bottomSheetView.findViewById(emojiIds[i]).setOnClickListener(v0 -> {
                        String currentText = commentEditText.getText().toString();
                        String emoji = emojis[finalI];
                        commentEditText.setText(currentText + emoji);
                        commentEditText.setSelection(commentEditText.getText().length()); // Move cursor to end
                    });
                }

                ImageView sendButton = bottomSheetView.findViewById(R.id.imageView_send);
                sendButton.setOnClickListener(v1 -> {
                    String commentText = commentEditText.getText().toString().trim();
                    if (!TextUtils.isEmpty(commentText)) {
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            Date commentDate = new Date();
                            long timestamp = commentDate.getTime();

                            FirebaseFirestore.getInstance().collection("users")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        UserModel user = documentSnapshot.toObject(UserModel.class);
                                        if (user != null) {
                                            String username = user.getUsername();
                                            String profilePic = user.getProfilePic();
                                            String id = UUID.randomUUID().toString();

                                            CommentModel newComment = new CommentModel(id, userId, username, profilePic, commentText, commentDate, timestamp);

                                            FirebaseFirestore.getInstance().collection("videos")
                                                    .document(video.getId())
                                                    .collection("comments")
                                                    .document(id) // Use unique ID
                                                    .set(newComment) // Use set to create a document with the specified ID
                                                    .addOnSuccessListener(documentReference -> {
                                                        commentList.add(0, newComment); // Add the new comment to the beginning of the list
                                                        commentEditText.setText("");
                                                        updateCommentCount(video);
                                                        adapter.notifyDataSetChanged();

                                                        TextView noCommentsTextView = bottomSheetView.findViewById(R.id.no_comments_text_view);
                                                        noCommentsTextView.setVisibility(View.GONE);
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(v.getContext(), "Failed to add comment", Toast.LENGTH_SHORT).show();
                                                    });
                                        }
                                    });
                        } else {
                            BottomSheetLoginFragment bottomSheetFragment = new BottomSheetLoginFragment();
                            bottomSheetFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), bottomSheetFragment.getTag());
                        }
                    } else {
                        Toast.makeText(v.getContext(), "Please enter a comment", Toast.LENGTH_SHORT).show();
                    }
                });

                bottomSheetDialog.show();
            });

            // Add touch listener to pause/play video
            binding.videoView.setOnTouchListener(new View.OnTouchListener() {
                private Handler handler = new Handler();
                private boolean longPressDetected = false;
                private float startX, startY;
                private static final int LONG_PRESS_TIMEOUT = 1000;
                private static final int MAX_MOVEMENT = 20;

                @Override
                public boolean onTouch(View view, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            startX = event.getX();
                            startY = event.getY();
                            longPressDetected = false;
                            handler.postDelayed(longPressRunnable, LONG_PRESS_TIMEOUT);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            float currentX = event.getX();
                            float currentY = event.getY();
                            if (Math.abs(currentX - startX) > MAX_MOVEMENT || Math.abs(currentY - startY) > MAX_MOVEMENT) {
                                handler.removeCallbacks(longPressRunnable);
                            }
                            return true;

                        case MotionEvent.ACTION_UP:
                            if (!longPressDetected) {
                                togglePlayback();
                            }
                            handler.removeCallbacks(longPressRunnable);
                            return true;

                        case MotionEvent.ACTION_CANCEL:
                            handler.removeCallbacks(longPressRunnable);
                            return true;

                        default:
                            return false;
                    }
                }

                private Runnable longPressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        longPressDetected = true;
                        showBottomSheet(itemView.getContext(), video);
                    }
                };
            });

            binding.more.setOnClickListener(view -> {
                showMoreOptions(video);
            });
        }

        private void showBottomSheet(Context context, VideoModel videoModel) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.ThemeOverlay_App_BottomSheetDialog);
            LayoutInflater inflater = LayoutInflater.from(context);
            View bottomSheetView = inflater.inflate(R.layout.bottom_sheet_layout, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            bottomSheetView.findViewById(R.id.report_option).setOnClickListener(v -> {
                // Handle report action
                showBottomSheetReport(context, videoModel);
                bottomSheetDialog.dismiss();
            });

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Recuperar o estado do botão de download
            db.collection("users")
                    .document(videoModel.getUploaderId()) // Obtém o uploader pelo ID do vídeo
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            Boolean isDownloadEnabled = documentSnapshot.getBoolean("downloadEnabled");

                            // Verifica se a configuração do uploader permite download
                            if (isDownloadEnabled != null && isDownloadEnabled) {
                                bottomSheetView.findViewById(R.id.layout_save).setVisibility(View.VISIBLE);
                            } else {
                                bottomSheetView.findViewById(R.id.layout_save).setVisibility(View.GONE);
                            }
                        } else {
                            // Caso o documento do uploader não exista
                            bottomSheetView.findViewById(R.id.layout_save).setVisibility(View.VISIBLE);
                        }
                    });

            bottomSheetView.findViewById(R.id.save_option).setOnClickListener(v -> {
                // Handle save action
                String videoUrl = videoModel.getVideoUrl();
                if (videoUrl != null) {
                    downloadAndProcessVideo(context, videoUrl);
                } else {
                    Toast.makeText(context, "Video URL is null", Toast.LENGTH_SHORT).show();
                }
                bottomSheetDialog.dismiss();
            });

            bottomSheetView.findViewById(R.id.share_option).setOnClickListener(v -> {
                // Handle share action
                shareVideo(context, videoModel, binding.shareCount);
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        }

        private void showBottomSheetReport(Context context, VideoModel videoModel) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.ThemeOverlay_App_BottomSheetDialog);
            LayoutInflater inflater = LayoutInflater.from(context);
            View bottomSheetView = inflater.inflate(R.layout.bottom_sheet_report_layout, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            RadioGroup radioGroupReasons = bottomSheetView.findViewById(R.id.radioGroupReasons);
            Button buttonReport = bottomSheetView.findViewById(R.id.buttonReport);

            buttonReport.setOnClickListener(v -> {
                // Obtenha o motivo selecionado
                int selectedId = radioGroupReasons.getCheckedRadioButtonId();
                String reason = "";

                if (selectedId == R.id.radioButton1) {
                    reason = context.getString(R.string.report_reason_pornographic);
                } else if (selectedId == R.id.radioButton2) {
                    reason = context.getString(R.string.report_reason_harmful_to_minors);
                } else if (selectedId == R.id.radioButton3) {
                    reason = context.getString(R.string.report_reason_criminal_activities);
                } else if (selectedId == R.id.radioButton4) {
                    reason = context.getString(R.string.report_reason_spam);
                } else if (selectedId == R.id.radioButton5) {
                    reason = context.getString(R.string.report_reason_hate_speech);
                } else if (selectedId == R.id.radioButton6) {
                    reason = context.getString(R.string.report_reason_harassment);
                } else if (selectedId == R.id.radioButton7) {
                    reason = context.getString(R.string.report_reason_violent);
                } else if (selectedId == R.id.radioButton8) {
                    reason = context.getString(R.string.report_reason_dangerous_orgs);
                } else if (selectedId == R.id.radioButton9) {
                    reason = context.getString(R.string.report_reason_suicide);
                } else if (selectedId == R.id.radioButton10) {
                    reason = context.getString(R.string.report_reason_false_info);
                } else if (selectedId == R.id.radioButton11) {
                    reason = context.getString(R.string.report_reason_intellectual_property);
                } else {
                    Toast.makeText(context, context.getString(R.string.select_reason_prompt), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Implemente a lógica para relatar o vídeo com o motivo selecionado
                reportVideo(context, videoModel, reason);
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        }

        private void reportVideo(Context context, VideoModel videoModel, String reason) {
            // Incrementar a contagem de denúncias e registrar o motivo no Firebase Firestore
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoModel.getId())
                    .update(
                            "reportCount", FieldValue.increment(1),
                            "reasons", FieldValue.arrayUnion(reason) // Adicionar motivo à lista de motivos
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Vídeo relatado com sucesso: " + reason, Toast.LENGTH_SHORT).show();

                        // Verificar se o vídeo deve ser removido
                        checkAndRemoveVideoIfNecessary(context, videoModel.getId(), reason);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Falha ao relatar vídeo", Toast.LENGTH_SHORT).show();
                    });
        }

        private void checkAndRemoveVideoIfNecessary(Context context, String videoId, String reason) {
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        long reportCount = documentSnapshot.getLong("reportCount");

                        // Verificar se o vídeo atingiu 20 denúncias
                        if (reportCount >= 20) {
                            // Verificar o motivo mais recente da denúncia
                            List<String> reasons = (List<String>) documentSnapshot.get("reasons");
                            if (reasons != null && reasons.contains(reason)) {
                                // Implementar a lógica para remover o vídeo
                                removeVideoFromFirestore(context, videoId);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Tratar falha ao obter o documento do vídeo
                        Toast.makeText(context, "Falha ao verificar número de denúncias", Toast.LENGTH_SHORT).show();
                    });
        }

        private void removeVideoFromFirestore(Context context, String videoId) {
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, context.getString(R.string.video_removed_due_to_reports), Toast.LENGTH_SHORT).show();
                    });
        }

        private void downloadAndProcessVideo(Context context, String videoUrl) {
            new Thread(() -> {
                try {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(videoUrl).build();
                    Response response = client.newCall(request).execute();

                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        File localFile = new File(context.getCacheDir(), "video.mp4");
                        try (OutputStream outputStream = new FileOutputStream(localFile)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            // Video downloaded successfully
                            // Process the video
                            fetchUsernameAndProcessVideo(context, localFile.getAbsolutePath(), videoUrl);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Error saving the video", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Handle failed response
                        Toast.makeText(context, "Failed to download video", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Error downloading the video", Toast.LENGTH_SHORT).show();
                }
            }).start();
        }

        private void animationLike(){
            binding.liked.setVisibility(View.VISIBLE);
            binding.liked.setAnimation(AnimationUtils.loadAnimation(itemView.getContext(), R.anim.like));
            binding.liked.setAlpha(1f);

            binding.liked.animate()
                    .alpha(0f)
                    .setDuration(2000)
                    .withEndAction(() -> binding.liked.setVisibility(View.GONE)) // Define a visibilidade como GONE após a animação
                    .start();
        }

        private void showMoreOptions(VideoModel video) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(binding.getRoot().getContext(), R.style.ThemeOverlay_App_BottomSheetDialog);
            LayoutInflater inflater = LayoutInflater.from(binding.getRoot().getContext());
            View bottomSheetView = inflater.inflate(R.layout.bottom_sheet_more_options, null);
            bottomSheetDialog.setContentView(bottomSheetView);

            Spinner visibilitySpinner = bottomSheetView.findViewById(R.id.visibility_spinner);

            final String[] visibilityValues = {"public", "private"};
            String[] visibilityDisplayNames = binding.getRoot().getContext().getResources().getStringArray(R.array.visibility_display_names);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(binding.getRoot().getContext(),
                    android.R.layout.simple_spinner_item, visibilityDisplayNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            visibilitySpinner.setAdapter(adapter);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference videoRef = db.collection("videos").document(video.getId());

            videoRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String currentVisibility = documentSnapshot.getString("visibility");
                    if (currentVisibility != null) {
                        int position = getVisibilityPosition(visibilityValues, currentVisibility);
                        visibilitySpinner.setSelection(position);
                    }
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(binding.getRoot().getContext(), "Failed to load visibility", Toast.LENGTH_SHORT).show();
            });

            visibilitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedVisibility = visibilityValues[position];
                    updateVisibilityInFirestore(video.getId(), selectedVisibility);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Do nothing
                }
            });

            TextView deleteOption = bottomSheetView.findViewById(R.id.delete_option);
            deleteOption.setOnClickListener(v -> {
                deleteVideoFromBunnyCDN(video, StorageConfig.STORAGE_ZONE, StorageConfig.API_KEY);
                bottomSheetDialog.dismiss();
            });

            bottomSheetDialog.show();
        }

        private int getVisibilityPosition(String[] values, String currentVisibility) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equals(currentVisibility)) {
                    return i;
                }
            }
            return 0; // Default to the first item if not found
        }

        private void updateVisibilityInFirestore(String videoId, String visibility) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference videoRef = db.collection("videos").document(videoId);

            videoRef.update("visibility", visibility)
                    .addOnSuccessListener(aVoid -> {
                        // Optional: Show success message
                        // Toast.makeText(binding.getRoot().getContext(), "Visibility updated", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(binding.getRoot().getContext(), "Failed to update visibility", Toast.LENGTH_SHORT).show();
                    });
        }

        private void deleteVideoFromBunnyCDN(VideoModel video, String storageZone, String apiKey) {
            // Executar a requisição em uma thread separada
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                // Obter a URL do vídeo
                String url = video.getUrl();

                // Extrai o nome do vídeo do URL
                Uri uri = Uri.parse(url);
                String path = uri.getPath();

                if (path != null) {
                    // A URL do vídeo é algo como "https://my-videos-1.b-cdn.net/videos/1741970920035.mp4"
                    // Pegamos a parte após "/videos/"
                    String[] pathParts = path.split("/videos/");
                    if (pathParts.length > 1) {
                        String fileName = pathParts[1]; // Obtém o nome do arquivo, por exemplo: "1741970920035.mp4"

                        // Construir o endpoint para a API de exclusão do BunnyCDN
                        String bunnyCdnUrl = "https://storage.bunnycdn.com/" + storageZone + "/videos/" + fileName;

                        try {
                            // Iniciar a requisição para deletar o arquivo do BunnyCDN
                            URL urlDelete = new URL(bunnyCdnUrl);
                            HttpURLConnection connection = (HttpURLConnection) urlDelete.openConnection();
                            connection.setRequestMethod("DELETE");
                            connection.setRequestProperty("AccessKey", apiKey); // Sua chave de API do BunnyCDN
                            connection.setDoOutput(true);

                            // Enviar a requisição
                            int responseCode = connection.getResponseCode();
                            String responseMessage = connection.getResponseMessage();

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                // Sucesso - arquivo excluído do BunnyCDN
                                // Aqui você pode remover o vídeo do Firestore ou atualizar a interface
                                deleteVideoMetadataFromFirestore(video);
                            } else {
                                // Falha na exclusão do vídeo do BunnyCDN
                                ((Activity) itemView.getContext()).runOnUiThread(() -> {
                                    Toast.makeText(binding.getRoot().getContext(), "Falha ao excluir vídeo do BunnyCDN", Toast.LENGTH_SHORT).show();
                                });
                            }

                        } catch (IOException e) {
                            // Erro na requisição
                            ((Activity) itemView.getContext()).runOnUiThread(() -> {
                                Toast.makeText(binding.getRoot().getContext(), "Erro ao excluir vídeo do BunnyCDN", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }
                }
            });
        }

        private void deleteVideoMetadataFromFirestore(VideoModel video) {
            // Obter referência do Firestore para deletar os metadados
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference videoDocRef = firestore.collection("videos").document(video.getId());

            videoDocRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(binding.getRoot().getContext(), "Vídeo excluído com sucesso.", Toast.LENGTH_SHORT).show();
                        // Remover vídeo da interface ou do adaptador
                        ((VideoPagerAdapter) getBindingAdapter()).removeVideo(video);
                        // Fechar a atividade ou realizar outra ação necessária
                        ((Activity) binding.getRoot().getContext()).finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(binding.getRoot().getContext(), "Falha ao excluir metadados do vídeo", Toast.LENGTH_SHORT).show();
                    });
        }

        private void copyFileFromRes(int resId, String destPath) {
            try {
                InputStream inputStream = itemView.getContext().getResources().openRawResource(resId);
                FileOutputStream outputStream = new FileOutputStream(destPath);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }

                inputStream.close();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void fetchUsernameAndProcessVideo(Context context, String videoPath, String videoUrl) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Query to find the video document by videoUrl
            db.collection("videos").whereEqualTo("videoUrl", videoUrl).get().addOnSuccessListener(querySnapshot -> {
                if (!querySnapshot.isEmpty()) {
                    // Assuming there is only one document that matches the videoUrl
                    DocumentSnapshot videoDocument = querySnapshot.getDocuments().get(0);
                    String uploaderId = videoDocument.getString("uploaderId");

                    if (uploaderId != null) {
                        // Fetch user details
                        db.collection("users").document(uploaderId).get().addOnSuccessListener(userDocument -> {
                            if (userDocument.exists()) {
                                UserModel user = userDocument.toObject(UserModel.class);
                                if (user != null) {
                                    String username = user.getUsername(); // Assuming getName() returns the username
                                    // Process the video with the retrieved username
                                    processVideoWithFFmpeg(context, videoPath, username);
                                }
                            }
                        }).addOnFailureListener(e -> {

                        });
                    }
                }
            }).addOnFailureListener(e -> {

            });
        }


        private void processVideoWithFFmpeg(Context context, String videoPath, String username) {
            View processProgress = binding.cardViewProgress.findViewById(R.id.card_view_progress);
            processProgress.setVisibility(View.VISIBLE);

            File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
            if (externalFilesDir == null) {
                Toast.makeText(context, "Failed to access external files directory", Toast.LENGTH_SHORT).show();
                return;
            }

            String outputPath = new File(externalFilesDir, "output.mp4").getAbsolutePath();
            String watermarkPath = new File(externalFilesDir, "watermark.png").getAbsolutePath();
            String fontPath = new File(externalFilesDir, "roboto_default.ttf").getAbsolutePath();
            String concatFilePath = new File(externalFilesDir, "concat_list.txt").getAbsolutePath();
            String additionalVideoPath = new File(externalFilesDir, "video.mp4").getAbsolutePath();
            String thumbnailPath = new File(externalFilesDir, "thumbnail.png").getAbsolutePath();

            copyFileFromRes(R.drawable.watermark, watermarkPath);
            copyFileFromRes(R.raw.roboto_bold, fontPath);
            copyFileFromRes(R.raw.video, additionalVideoPath);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(concatFilePath))) {
                writer.write("file '" + outputPath + "'\n");
                writer.write("file '" + additionalVideoPath + "'\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            String[] processCommand = {
                    "-i", videoPath,
                    "-i", watermarkPath,
                    "-filter_complex",
                    "[1:v]scale=200:-1[w];" +
                            "[0:v][w]overlay=x='if(lt(mod(t\\,10)\\,5)\\,W/4\\,W-w-W/4)':y='if(lt(mod(t\\,10)\\,5)\\,H/4\\,H-h-H/4)'," +
                            "drawtext=text='@" + username + "':x='if(lt(mod(t\\,10)\\,5)\\,W/4\\,W-tw-W/4)':y='if(lt(mod(t\\,10)\\,5)\\,H/4\\,H-th-H/4)':fontsize=24:fontcolor=white:box=1:boxcolor=black@0.5:fontfile=" + fontPath,
                    "-c:v", "libx264",
                    "-preset", "slow",
                    "-crf", "18",
                    "-y", outputPath
            };

            FFmpeg.executeAsync(processCommand, new ExecuteCallback() {
                @Override
                public void apply(final long executionId, final int returnCode) {
                    if (returnCode == RETURN_CODE_SUCCESS) {

                        String[] thumbnailCommand = {
                                "-i", outputPath,
                                "-ss", "00:00:01.000",
                                "-vframes", "1",
                                "-q:v", "2",
                                thumbnailPath
                        };

                        FFmpeg.executeAsync(thumbnailCommand, new ExecuteCallback() {
                            @Override
                            public void apply(final long executionId, final int returnCode) {
                                if (returnCode == RETURN_CODE_SUCCESS) {

                                    String[] concatCommand = {
                                            "-f", "concat",
                                            "-safe", "0",
                                            "-i", concatFilePath,
                                            "-c", "copy",
                                            new File(externalFilesDir, "final_output.mp4").getAbsolutePath()
                                    };

                                    FFmpeg.executeAsync(concatCommand, new ExecuteCallback() {
                                        @Override
                                        public void apply(final long executionId, final int returnCode) {
                                            if (returnCode == RETURN_CODE_SUCCESS) {
                                                processProgress.setVisibility(View.GONE);

                                                // Renomeia o arquivo de saída final com um nome aleatório
                                                String randomFileName = "video_" + System.currentTimeMillis() + ".mp4";
                                                File finalOutputFile = new File(externalFilesDir, "final_output.mp4");
                                                File renamedFile = new File(externalFilesDir, randomFileName);
                                                boolean renamed = finalOutputFile.renameTo(renamedFile);

                                                if (renamed) {
                                                    // Mostra o bottom sheet com a miniatura
                                                    ThumbnailBottomSheetFragment bottomSheetFragment = new ThumbnailBottomSheetFragment(renamedFile.getAbsolutePath(), thumbnailPath);
                                                    bottomSheetFragment.show(((AppCompatActivity) context).getSupportFragmentManager(), bottomSheetFragment.getTag());

                                                    // Exclui os arquivos temporários após exibir o bottom sheet
                                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                        new File(outputPath).delete();
                                                        new File(watermarkPath).delete();
                                                        new File(concatFilePath).delete();
                                                        new File(additionalVideoPath).delete();
                                                        new File(thumbnailPath).delete();
                                                    }, 500);
                                                }
                                            }
                                        }
                                    });

                                }
                            }
                        });

                    }
                }
            });
        }

        private void shareVideo(final Context context, final VideoModel videoModel, final TextView shareCountView) {
            // Create a Bottom Sheet Dialog
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.layout_share_bottom_sheet, null);

            // Set custom text in Bottom Sheet
            TextView bottomSheetText = bottomSheetView.findViewById(R.id.bottomSheetText);
            bottomSheetText.setText("Compartilhe este vídeo!");

            // Share Button
            Button shareButton = bottomSheetView.findViewById(R.id.shareButton);
            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.video_share_text) + "\n\nhttps://www.vidbr.com.br/comments.html?videoId=" + videoModel.getVideoId());
                    shareIntent.setPackage("com.whatsapp"); // Set WhatsApp as the target package

                    // Store the video ID in SharedPreferences
                    SharedPreferences sharedPreferences = context.getSharedPreferences("VideoPrefs", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("last_shared_video_id", videoModel.getId());
                    editor.apply();

                    try {
                        ((Activity) context).startActivityForResult(Intent.createChooser(shareIntent, context.getString(R.string.share_video)), SHARE_REQUEST_CODE);
                    } catch (android.content.ActivityNotFoundException ex) {
                        // Exibir um Snackbar em vez de um Toast
                        Snackbar.make(((Activity) context).findViewById(android.R.id.content), context.getString(R.string.whatsapp_not_installed), Snackbar.LENGTH_SHORT).show();
                    }

                    bottomSheetDialog.dismiss(); // Close the Bottom Sheet
                }
            });

            // Copy Link Button
            Button copyLinkButton = bottomSheetView.findViewById(R.id.copyLinkButton);
            copyLinkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Copy the video URL to the clipboard
                    String videoUrl = "https://www.vidbr.com.br/comments.html?videoId=" + videoModel.getVideoId();
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Video URL", videoUrl);
                    clipboard.setPrimaryClip(clip);

                    // Show a Snackbar to indicate the URL has been copied
                    Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show();

                    bottomSheetDialog.dismiss(); // Close the Bottom Sheet
                }
            });

            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show(); // Show the Bottom Sheet
        }

        private void updateShareCount(String videoId) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firestore.collection("videos").document(videoId).addSnapshotListener((documentSnapshot, error) -> {
                if (error == null && documentSnapshot != null && documentSnapshot.exists()) {
                    Long shareCount = documentSnapshot.getLong("shareCount");
                    binding.shareCount.setText(shareCount != null ? String.valueOf(shareCount) : "0");
                }
            });
        }

        private void updateProgressText(long currentPosition) {
            if (player == null) {
                return; // Early exit if player is not initialized
            }

            long duration = player.getDuration();

            // Convert milliseconds to MM:SS format
            String currentTime = formatTime(currentPosition);
            String totalTime = formatTime(duration);

            // Update text views on the UI thread
            binding.currentTimeTextView.post(() -> {
                binding.currentTimeTextView.setText(currentTime);
                binding.dividerTimeTextView.setText(" /");
                binding.totalTimeTextView.setText(totalTime);
            });
        }
        // Método para formatar o tempo em MM:SS
        private String formatTime(long millis) {
            long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
            long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }

        private void setCaptionWithHashtags(final String caption) {
            final String fullText = caption;
            final TextView captionView = binding.captionView;
            final TextView moreButton = binding.moreButton;

            // Create initial SpannableString
            final SpannableString spannableString = new SpannableString(fullText);

            // Set up hashtag pattern and clickable spans
            Pattern pattern = Pattern.compile("#(\\w+)");
            Matcher matcher = pattern.matcher(fullText);

            // Store spans for later use
            Map<Integer, String> hashtagPositions = new HashMap<>();

            while (matcher.find()) {
                final String currentHashtag = matcher.group().substring(1);
                int start = matcher.start();
                int end = matcher.end();
                hashtagPositions.put(start, currentHashtag);

                // Apply spans to the SpannableString
                StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                spannableString.setSpan(boldSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        Intent intent = new Intent(widget.getContext(), HashtagVideosActivity.class);
                        intent.putExtra("hashtag", currentHashtag);
                        widget.getContext().startActivity(intent);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(Color.WHITE);
                    }
                };

                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            if (fullText.length() > 30) {
                String shortenedText = fullText.substring(0, 30) + " ...";
                final SpannableString shortenedSpannable = new SpannableString(shortenedText);

                // Adjust hashtag positions for shortened text
                for (Map.Entry<Integer, String> entry : hashtagPositions.entrySet()) {
                    int originalStart = entry.getKey();
                    String hashtag = entry.getValue();

                    // Calculate new start position for the shortened text
                    int newStart = originalStart < 30 ? originalStart : (30 + 3); // 3 for the length of " ..."
                    int newEnd = newStart + hashtag.length() + 1; // +1 for the "#" character

                    if (newStart >= 0 && newEnd <= shortenedText.length()) {
                        StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
                        shortenedSpannable.setSpan(boldSpan, newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        ClickableSpan clickableSpan = new ClickableSpan() {
                            @Override
                            public void onClick(@NonNull View widget) {
                                Intent intent = new Intent(widget.getContext(), HashtagVideosActivity.class);
                                intent.putExtra("hashtag", hashtag);
                                widget.getContext().startActivity(intent);
                            }

                            @Override
                            public void updateDrawState(@NonNull TextPaint ds) {
                                super.updateDrawState(ds);
                                ds.setUnderlineText(false);
                                ds.setColor(Color.WHITE);
                            }
                        };

                        shortenedSpannable.setSpan(clickableSpan, newStart, newEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                moreButton.setVisibility(View.VISIBLE);
                moreButton.setText(R.string.more_button_text);

                moreButton.setOnClickListener(new View.OnClickListener() {
                    boolean isExpanded = false;

                    @Override
                    public void onClick(View v) {
                        if (isExpanded) {
                            captionView.setText(shortenedSpannable);
                            moreButton.setText(R.string.more_button_text);
                        } else {
                            captionView.setText(spannableString);
                            moreButton.setText(R.string.hide_button_text);
                        }
                        isExpanded = !isExpanded;
                    }
                });

                captionView.setText(shortenedSpannable);
                captionView.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                moreButton.setVisibility(View.GONE);
                captionView.setText(spannableString);
                captionView.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

        private void updateCommentCount(VideoModel videoModel) {
            // Atualiza o número de comentários no documento Firestore do vídeo
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoModel.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            int currentCommentCount = documentSnapshot.getLong("commentCount") != null ? documentSnapshot.getLong("commentCount").intValue() : 0;
                            // Incrementa o número de comentários
                            currentCommentCount++;
                            // Atualiza o número de comentários no Firestore
                            int finalCurrentCommentCount = currentCommentCount;
                            documentSnapshot.getReference().update("commentCount", currentCommentCount)
                                    .addOnSuccessListener(aVoid -> {
                                        // Atualiza o TextView com o novo número de comentários
                                        binding.commentNumber.setText(String.valueOf(finalCurrentCommentCount));
                                    })
                                    .addOnFailureListener(e -> {
                                        // Handle failure to update comment count in Firestore
                                        Toast.makeText(binding.commentNumber.getContext(), "Falha ao atualizar o número de comentários", Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle failure to fetch Firestore information
                        Toast.makeText(binding.commentNumber.getContext(), "Falha ao obter o número de comentários", Toast.LENGTH_SHORT).show();
                    });
        }

        private void listenForCommentChanges(VideoModel videoModel) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firestore.collection("videos")
                    .document(videoModel.getId())
                    .addSnapshotListener((documentSnapshot, e) -> {
                        if (e != null) {
                            Toast.makeText(binding.commentNumber.getContext(), "Erro ao escutar mudanças", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (documentSnapshot != null && documentSnapshot.exists()) {
                            Long commentCount = documentSnapshot.getLong("commentCount");
                            if (commentCount != null) {
                                binding.commentNumber.setText(formatLikesCount(commentCount.intValue()));
                            }
                        }
                    });
        }

        private void onLikeClick(VideoModel video) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                String userId = currentUser.getUid();
                animationLike();

                db.collection("videos")
                        .document(video.getId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                                Long likesCount = documentSnapshot.getLong("likesCount");

                                if (likedBy != null) {
                                    if (likedBy.contains(userId)) {
                                        // User has liked the video, so unlike it
                                        likedBy.remove(userId);
                                        likesCount = (likesCount != null) ? likesCount - 1 : 0;
                                    } else {
                                        // User has not liked the video, so like it
                                        if (likedBy == null) {
                                            likedBy = new ArrayList<>();
                                        }
                                        likedBy.add(userId);
                                        likesCount = (likesCount != null) ? likesCount + 1 : 1;
                                    }

                                    updateLikesInDatabase(video.getId(), likedBy, likesCount);
                                }
                            }
                        });
            } else {
                BottomSheetLoginFragment bottomSheetFragment = new BottomSheetLoginFragment();
                bottomSheetFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), bottomSheetFragment.getTag());
            }
        }

        private void updateLikesInDatabase(String videoId, List<String> likedBy, Long likesCount) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("videos")
                    .document(videoId)
                    .update("likedBy", likedBy, "likesCount", likesCount)
                    .addOnSuccessListener(aVoid -> {
                        // Update UI here based on the current like state
                        binding.likeIcon.setImageResource(likedBy.contains(FirebaseAuth.getInstance().getCurrentUser().getUid()) ? R.drawable.liked : R.drawable.like);
                        binding.likesCount.setText(String.valueOf(likesCount));
                    });
        }

        private void updateLikesInfo(VideoModel video) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();

                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build();
                firestore.setFirestoreSettings(settings);
                firestore.collection("videos")
                        .document(video.getId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                List<String> likedBy = (List<String>) documentSnapshot.get("likedBy");
                                Long likesCount = documentSnapshot.getLong("likesCount");

                                // Atualize o ícone de like
                                if (likedBy != null && likedBy.contains(userId)) {
                                    binding.likeIcon.setImageResource(R.drawable.liked);
                                } else {
                                    binding.likeIcon.setImageResource(R.drawable.like);
                                }

                                // Atualize a contagem de likes
                                if (likesCount != null) {
                                    binding.likesCount.setText(formatLikesCount(Math.toIntExact(likesCount)));
                                }
                            } else {
                                // Trate o caso em que o documento do vídeo não existe
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Trate falhas ao verificar se o usuário curtiu o vídeo
                            binding.likeIcon.setImageResource(R.drawable.like);
                        });
            }
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

        private String getCurrentUserId() {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                return currentUser.getUid();
            }
            return null;
        }

        public void releasePlayer() {
            if (player != null) {
                player.release();
            }
        }

        public void pauseVideo() {
            if (player != null && player.isPlaying()) {
                player.pause();
            }
        }
    }
}
