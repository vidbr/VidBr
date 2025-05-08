package com.video.vidbr.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.CommentModel;
import com.video.vidbr.model.UserModel;
import com.video.vidbr.model.VideoModel;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
    private List<CommentModel> commentList;
    private Context context;
    private String videoId;
    private TextView commentNumber;

    public CommentsAdapter(List<CommentModel> commentList, String videoId) {
        this.commentList = commentList;
        this.videoId = videoId;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        CommentModel comment = commentList.get(position);
        holder.bindComment(comment);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public class CommentViewHolder extends RecyclerView.ViewHolder {
        private ImageView userImageView;
        private TextView usernameTextView;
        private ImageView verifiedIcon;
        private ImageView verifiedGold;
        private TextView commentTextView;
        private TextView dateTextView;

        public CommentViewHolder(View itemView) {
            super(itemView);
            userImageView = itemView.findViewById(R.id.imageView_user);
            usernameTextView = itemView.findViewById(R.id.textView_username);
            verifiedIcon = itemView.findViewById(R.id.verified_icon);
            verifiedGold = itemView.findViewById(R.id.verifiedGold);
            commentTextView = itemView.findViewById(R.id.textView_comment);
            dateTextView = itemView.findViewById(R.id.textView_comment_date);
            commentNumber = itemView.findViewById(R.id.commentNumber);
        }

        public void bindComment(CommentModel comment) {
            usernameTextView.setText(comment.getUsername());
            // Buscar dados do usuário no Firestore para verificar status de verificação
            FirebaseFirestore.getInstance().collection("users")
                    .document(comment.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            UserModel user = documentSnapshot.toObject(UserModel.class);
                            if (user != null) {
                                verifiedIcon.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                                verifiedGold.setVisibility(user.isVerifiedGold() ? View.VISIBLE : View.GONE);
                            }
                        }
                    });
            commentTextView.setText(comment.getCommentText());
            dateTextView.setText(getTimeAgo(comment.getTimestamp()));

            userImageView.setOnClickListener(view -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("profile_user_id", comment.getUserId());
                context.startActivity(intent);
            });

            usernameTextView.setOnClickListener(view -> {
                Intent intent = new Intent(context, ProfileActivity.class);
                intent.putExtra("profile_user_id", comment.getUserId());
                context.startActivity(intent);
            });

            Glide.with(context)
                    .load(comment.getProfilePic())
                    .apply(new RequestOptions().placeholder(R.drawable.icon_account_circle))
                    .circleCrop()
                    .into(userImageView);

            itemView.setOnLongClickListener(v -> {
                showBottomSheet(comment);
                return true;
            });
        }

        private void showBottomSheet(CommentModel comment) {
            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.ThemeOverlay_App_BottomSheetDialog);
            View bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_comment_options, null);
            bottomSheetDialog.setContentView(bottomSheetView);
            bottomSheetDialog.show();

            TextView textViewCopy = bottomSheetView.findViewById(R.id.text_view_copy);
            TextView textViewDelete = bottomSheetView.findViewById(R.id.text_view_delete);

            if (FirebaseAuth.getInstance().getCurrentUser() != null &&
                    FirebaseAuth.getInstance().getCurrentUser().getUid().equals(comment.getUserId())) {
                textViewDelete.setVisibility(View.VISIBLE);
            } else {
                textViewDelete.setVisibility(View.GONE);
            }

            textViewCopy.setOnClickListener(v -> {
                copyComment(comment.getCommentText());
                bottomSheetDialog.dismiss();
            });

            textViewDelete.setOnClickListener(v -> {
                deleteComment(comment, getAdapterPosition());
                bottomSheetDialog.dismiss();
            });
        }

        private void copyComment(String commentText) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Comentário", commentText);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Comentário copiado", Toast.LENGTH_SHORT).show();
        }

        private void deleteComment(CommentModel comment, int position) {
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoId)
                    .collection("comments")
                    .document(comment.getId())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        commentList.remove(position);
                        notifyItemRemoved(position);
                        updateCommentCount(new VideoModel(videoId));
                    })
                    .addOnFailureListener(e -> Toast.makeText(context, "Falha ao excluir comentário", Toast.LENGTH_SHORT).show());
        }

        private void updateCommentCount(VideoModel videoModel) {
            if (videoModel.getVideoId() == null) {
                Toast.makeText(context, "ID do vídeo é nulo", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseFirestore.getInstance().collection("videos")
                    .document(videoModel.getVideoId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            int currentCommentCount = documentSnapshot.getLong("commentCount") != null ? documentSnapshot.getLong("commentCount").intValue() : 0;
                            if (currentCommentCount > 0) {
                                currentCommentCount--;
                                int finalCurrentCommentCount = currentCommentCount;
                                documentSnapshot.getReference().update("commentCount", currentCommentCount)
                                        .addOnSuccessListener(aVoid -> {
                                            // Atualiza o TextView com o novo número de comentários
                                            if (commentNumber != null) {
                                                commentNumber.setText(String.valueOf(finalCurrentCommentCount)); // Isso deve funcionar agora
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            // Handle failure to update comment count in Firestore
                                            Toast.makeText(commentNumber.getContext(), "Falha ao atualizar o número de comentários", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }
                    });
        }

        private String getTimeAgo(long timestamp) {
            long currentTime = System.currentTimeMillis();
            long timeDifference = currentTime - timestamp;

            if (timeDifference < TimeUnit.SECONDS.toMillis(1)) {
                return "agora mesmo";
            } else if (timeDifference < TimeUnit.MINUTES.toMillis(1)) {
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timeDifference);
                return seconds + " s atrás";
            } else if (timeDifference < TimeUnit.HOURS.toMillis(1)) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timeDifference);
                return minutes + " min atrás";
            } else if (timeDifference < TimeUnit.DAYS.toMillis(1)) {
                long hours = TimeUnit.MILLISECONDS.toHours(timeDifference);
                return hours + " h atrás";
            } else if (timeDifference < TimeUnit.DAYS.toMillis(7)) {
                long days = TimeUnit.MILLISECONDS.toDays(timeDifference);
                return days + " d atrás";
            } else {
                Date date = new Date(timestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return sdf.format(date);
            }
        }
    }
}
