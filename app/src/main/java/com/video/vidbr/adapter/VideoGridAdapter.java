package com.video.vidbr.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.R;
import com.video.vidbr.model.UserModel;
import com.video.vidbr.model.VideoModel;

import java.lang.ref.WeakReference;
import java.util.List;

public class VideoGridAdapter extends BaseAdapter {
    private final WeakReference<Context> contextRef;
    private final List<VideoModel> videos;

    public VideoGridAdapter(Context context, List<VideoModel> videos) {
        this.contextRef = new WeakReference<>(context);
        this.videos = videos;
    }

    @Override
    public int getCount() {
        return videos.size();
    }

    @Override
    public Object getItem(int position) {
        return videos.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(contextRef.get()).inflate(R.layout.grid_item_video, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.thumbnail = convertView.findViewById(R.id.video_thumbnail);
            viewHolder.title = convertView.findViewById(R.id.video_title);
            viewHolder.likesCount = convertView.findViewById(R.id.likes_count);
            viewHolder.userPhoto = convertView.findViewById(R.id.user_photo);
            viewHolder.userName = convertView.findViewById(R.id.user_name);
            viewHolder.verified = convertView.findViewById(R.id.verified_icon);
            viewHolder.verifiedGold = convertView.findViewById(R.id.verifiedGold);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        VideoModel video = videos.get(position);

        // Load the video thumbnail using Glide
        Context context = contextRef.get();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (!activity.isDestroyed() && !activity.isFinishing()) {
                Glide.with(context)
                        .load(video.getVideoUrl())
                        .apply(RequestOptions.bitmapTransform(new RoundedCorners(16))).diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(viewHolder.thumbnail);
            }
        }

        String title = video.getTitle();
        if (title.length() > 37) {
            title = title.substring(0, 37) + "...";
        }
        viewHolder.title.setText(title);
        viewHolder.likesCount.setText(formatLikesCount(video.getLikesCount()));

        // Load the user information from Firebase Firestore
        FirebaseFirestore.getInstance().collection("users")
                .document(video.getUploaderId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    UserModel user = documentSnapshot.toObject(UserModel.class);
                    if (user != null) {
                        viewHolder.userName.setText(user.getUsername());
                        if (context instanceof Activity) {
                            Activity activity = (Activity) context;
                            if (!activity.isDestroyed() && !activity.isFinishing()) {
                                Glide.with(context)
                                        .load(user.getProfilePic())
                                        .apply(new RequestOptions().placeholder(R.drawable.icon_account_circle))
                                        .circleCrop()
                                        .into(viewHolder.userPhoto);
                            }
                        }
                        viewHolder.verified.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
                        viewHolder.verifiedGold.setVisibility(user.isVerifiedGold() ? View.VISIBLE : View.GONE);
                    }
                });

        return convertView;
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

    private static class ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView likesCount;
        ImageView userPhoto;
        TextView userName;
        ImageView verified;
        ImageView verifiedGold;
    }
}
