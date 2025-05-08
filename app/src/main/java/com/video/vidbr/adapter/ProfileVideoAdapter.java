package com.video.vidbr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.video.vidbr.databinding.ProfileVideoItemRowBinding;
import com.video.vidbr.model.VideoModel;

import java.util.List;

public class ProfileVideoAdapter extends RecyclerView.Adapter<ProfileVideoAdapter.VideoViewHolder> {

    private List<VideoModel> videoList;
    private OnItemClickListener listener;
    private Context context;

    public ProfileVideoAdapter(List<VideoModel> videoList, Context context) {
        this.videoList = videoList;
        this.context = context;
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public class VideoViewHolder extends RecyclerView.ViewHolder {

        private ProfileVideoItemRowBinding binding;

        public VideoViewHolder(ProfileVideoItemRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;

            binding.thumbnailImageView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onItemClick(position);
                }
            });
        }

        public void bind(VideoModel video) {
            // Use Glide to load video thumbnail
            Glide.with(binding.thumbnailImageView.getContext())
                    .load(video.getUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(binding.thumbnailImageView);

            // Format and display the like count
            String formattedLikesCount = formatLikesCount(video.getLikesCount());
            binding.likeCount.setText(formattedLikesCount);
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

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ProfileVideoItemRowBinding binding = ProfileVideoItemRowBinding.inflate(inflater, parent, false);
        return new VideoViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoModel video = videoList.get(position);
        holder.bind(video);
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    // Method to update the data list and refresh the adapter
    public void updateVideoList(List<VideoModel> newVideoList) {
        this.videoList = newVideoList;
        notifyDataSetChanged();
    }
}
