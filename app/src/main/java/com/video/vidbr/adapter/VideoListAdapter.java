package com.video.vidbr.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.video.vidbr.CacheDataSourceFactory;
import com.video.vidbr.R;
import com.video.vidbr.model.VideoModel;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Environment;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.view.MotionEvent;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;
import androidx.lifecycle.MutableLiveData;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.TimeBar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.video.vidbr.HashtagVideosActivity;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.ThumbnailBottomSheetFragment;
import com.video.vidbr.VideoPlayerManager;
import com.video.vidbr.databinding.VideoItemRowBinding;
import com.video.vidbr.fragments.BottomSheetLoginFragment;
import com.video.vidbr.model.CommentModel;
import com.video.vidbr.model.UserModel;
import com.video.vidbr.model.VideoModel;
import com.video.vidbr.util.TimeUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class VideoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int SHARE_REQUEST_CODE = 1001;

    private static final int VIDEO_TYPE = 0;
    private static final int AD_TYPE = 1;
    private Context context;
    private List<Object> itemList; // List to store both VideoModel and AdView

    public VideoListAdapter(Context context, List<Object> itemList) {
        this.context = context;
        this.itemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        if (itemList.get(position) instanceof VideoModel) {
            return VIDEO_TYPE;
        } else {
            return AD_TYPE;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIDEO_TYPE) {
            VideoItemRowBinding binding = VideoItemRowBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new VideoViewHolder(binding);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_ad, parent, false);
            return new AdViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIDEO_TYPE) {
            VideoModel video = (VideoModel) itemList.get(position);
            VideoViewHolder videoHolder = (VideoViewHolder) holder;
            videoHolder.bindVideo(video);
        } else {
            AdViewHolder adHolder = (AdViewHolder) holder;
            // Load ad
            AdRequest adRequest = new AdRequest.Builder().build();
            adHolder.adView.loadAd(adRequest);
        }
    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).releasePlayer();
        }
        super.onViewRecycled(holder);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).player.setPlayWhenReady(true);
            ((VideoViewHolder) holder).player.setRepeatMode(Player.REPEAT_MODE_ALL);
            ((VideoViewHolder) holder).binding.pauseIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder instanceof VideoViewHolder) {
            ((VideoViewHolder) holder).player.setPlayWhenReady(false);
        }
    }

    public void updateVideos(List<Object> newVideoList) {
        this.itemList = newVideoList;
        notifyDataSetChanged(); // Notify the adapter of data changes
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        private final VideoItemRowBinding binding;
        private SimpleExoPlayer player;
        private final MutableLiveData<VideoModel> videoModelLiveData = new MutableLiveData<>();

        private boolean isExpanded = false;
        private TextView captionView;
        private Button moreButton;
        private SpannableString spannableString;

        public VideoViewHolder(VideoItemRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

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

        private void bindVideo(VideoModel videoModel) {
            // Fetch uploader info
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firestore.collection("users")
                    .document(videoModel.getUploaderId())
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
                                                binding.profileIcon.setImageResource(R.drawable.icon_account_circle); // Default image
                                                return;
                                            }

                                            int borderWidth = 6;
                                            int borderColor = Color.WHITE;
                                            Bitmap borderedBitmap = Bitmap.createBitmap(resource.getWidth() + borderWidth * 2,
                                                    resource.getHeight() + borderWidth * 2, resource.getConfig());
                                            Canvas canvas = new Canvas(borderedBitmap);

                                            Paint borderPaint = new Paint();
                                            borderPaint.setColor(borderColor);
                                            borderPaint.setStyle(Paint.Style.FILL);

                                            float radius = (Math.min(resource.getWidth(), resource.getHeight()) / 2f) + borderWidth;
                                            canvas.drawCircle(borderedBitmap.getWidth() / 2f, borderedBitmap.getHeight() / 2f, radius, borderPaint);

                                            canvas.drawBitmap(resource, borderWidth, borderWidth, null);

                                            RoundedBitmapDrawable circularDrawable = RoundedBitmapDrawableFactory.create(binding.profileIcon.getResources(), borderedBitmap);
                                            circularDrawable.setCircular(true);
                                            binding.profileIcon.setImageDrawable(circularDrawable);
                                        }

                                        @Override
                                        public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                            binding.profileIcon.setImageResource(R.drawable.icon_account_circle); // Default image
                                        }
                                    });

                            binding.profileIcon.setOnClickListener(view -> {
                                Intent intent = new Intent(binding.userDetailLayout.getContext(), ProfileActivity.class);
                                intent.putExtra("profile_user_id", userModel.getId());
                                binding.userDetailLayout.getContext().startActivity(intent);
                            });
                            binding.usernameView.setOnClickListener(view -> {
                                Intent intent = new Intent(binding.userDetailLayout.getContext(), ProfileActivity.class);
                                intent.putExtra("profile_user_id", userModel.getId());
                                binding.userDetailLayout.getContext().startActivity(intent);
                            });

                            // Verifica se os downloads estão ativados sem fazer outra consulta
                            Boolean isDownloadEnabled = documentSnapshot.getBoolean("downloadEnabled");
                            binding.download.setVisibility((isDownloadEnabled != null && isDownloadEnabled) ? View.VISIBLE : View.GONE);
                        }
                    });

            setCaptionWithHashtags(videoModel.getTitle());
            listenForCommentChanges(videoModel);

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

            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoModel.getVideoUrl()));

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

            binding.likeIcon.setOnClickListener(view -> onLikeClick(videoModel));

            binding.share.setOnClickListener(view -> shareVideo(view.getContext(), videoModel, binding.shareCount));
            updateShareCount(videoModel.getId());

            binding.download.setOnClickListener(view -> {
                // Handle save action
                String videoUrl = videoModel.getVideoUrl();
                if (videoUrl != null) {
                    downloadAndProcessVideo(view.getContext(), videoUrl);
                }
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

            videoModelLiveData.setValue(videoModel);
            String formattedLikesCount = formatLikesCount(videoModel.getCommentCount());

            binding.videoView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);

            binding.commentNumber.setText(formattedLikesCount);

            Timestamp createdTime = videoModel.getCreatedTime();
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
                CommentsAdapter adapter = new CommentsAdapter(commentList, videoModel.getVideoId());
                recyclerView.setAdapter(adapter);

                progressBar.setVisibility(View.VISIBLE); // Show progress bar while loading comments

                FirebaseFirestore.getInstance().collection("videos")
                        .document(videoModel.getId())
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
                                        .document(videoModel.getId())
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
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
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
                                                    .document(videoModel.getId())
                                                    .collection("comments")
                                                    .document(id) // Use unique ID
                                                    .set(newComment) // Use set to create a document with the specified ID
                                                    .addOnSuccessListener(documentReference -> {
                                                        commentList.add(0, newComment); // Add the new comment to the beginning of the list
                                                        commentEditText.setText("");
                                                        updateCommentCount(videoModel);
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
                        showBottomSheet(itemView.getContext(), videoModel);
                    }
                };
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
                        Toast.makeText(context, "Video successfully reported: " + reason, Toast.LENGTH_SHORT).show();

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
                                } else {

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
            bottomSheetText.setText(context.getString(R.string.share_video_text));

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

        private void requestPermissions(Context ctx) {
            if (ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) ctx, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
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

        private void onLikeClick(VideoModel videoModel) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                binding.liked.setVisibility(View.VISIBLE);
                binding.liked.setAnimation(AnimationUtils.loadAnimation(itemView.getContext(), R.anim.like));
                binding.liked.setAlpha(1f);

                binding.liked.animate()
                        .alpha(0f)
                        .setDuration(2000)
                        .withEndAction(() -> binding.liked.setVisibility(View.GONE)) // Define a visibilidade como GONE após a animação
                        .start();

                String currentUserId = currentUser.getUid();

                List<String> likedBy = videoModel.getLikedBy();
                if (likedBy == null) {
                    likedBy = new ArrayList<>();
                }

                if (likedBy.contains(currentUserId)) {
                    likedBy.remove(currentUserId);
                    videoModel.setLikesCount(videoModel.getLikesCount() - 1);
                } else {
                    likedBy.add(currentUserId);
                    videoModel.setLikesCount(videoModel.getLikesCount() + 1);
                }

                FirebaseFirestore.getInstance().collection("videos")
                        .document(videoModel.getId())
                        .update("likedBy", likedBy, "likesCount", videoModel.getLikesCount())
                        .addOnSuccessListener(aVoid -> videoModelLiveData.setValue(videoModel))
                        .addOnFailureListener(e -> {
                            // Handle failure to update data in Firestore
                        });

            } else {
                BottomSheetLoginFragment bottomSheetFragment = new BottomSheetLoginFragment();
                bottomSheetFragment.show(((AppCompatActivity) itemView.getContext()).getSupportFragmentManager(), bottomSheetFragment.getTag());
            }
        }

        // Dentro do método updateLikesInfo
        private void updateLikesInfo(VideoModel videoModel) {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .build();
            firestore.setFirestoreSettings(settings);
            firestore.collection("videos")
                    .document(videoModel.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            VideoModel updatedVideo = documentSnapshot.toObject(VideoModel.class);
                            if (updatedVideo != null) {
                                videoModel.setLikesCount(updatedVideo.getLikesCount());
                                List<String> likedBy = updatedVideo.getLikedBy();
                                if (likedBy == null) {
                                    likedBy = new ArrayList<>();
                                }
                                videoModel.setLikedBy(likedBy);

                                // Atualize o ícone do botão de curtir com base na presença do usuário atual na lista de curtidas
                                boolean currentUserLiked = likedBy.contains(getCurrentUserId());
                                binding.likeIcon.setImageResource(currentUserLiked ? R.drawable.liked : R.drawable.like);

                                // Atualize o contador de curtidas
                                binding.likesCount.setText(formatLikesCount(updatedVideo.getLikesCount()));

                                // Não interrompa a reprodução do vídeo durante a atualização
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Lide com a falha em buscar informações do Firestore
                    });
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

        private void releasePlayer() {
            if (player != null) {
                player.release();
                player = null;
            }
        }
    }

    public static class AdViewHolder extends RecyclerView.ViewHolder {
        AdView adView;

        public AdViewHolder(@NonNull View itemView) {
            super(itemView);
            adView = itemView.findViewById(R.id.adView);
        }
    }
}
