package com.video.vidbr.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.vidbr.R;

import java.util.List;

public class HashtagSuggestionAdapter extends RecyclerView.Adapter<HashtagSuggestionAdapter.ViewHolder> {
    private List<String> hashtags;
    private OnHashtagClickListener listener;

    public interface OnHashtagClickListener {
        void onHashtagClick(String hashtag);
    }

    public HashtagSuggestionAdapter(List<String> hashtags, OnHashtagClickListener listener) {
        this.hashtags = hashtags;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hashtag, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String hashtag = hashtags.get(position);
        holder.hashtagTextView.setText(hashtag);
        holder.itemView.setOnClickListener(v -> listener.onHashtagClick(hashtag));
    }

    @Override
    public int getItemCount() {
        return hashtags.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hashtagTextView;

        ViewHolder(View itemView) {
            super(itemView);
            hashtagTextView = itemView.findViewById(R.id.hashtag_text);
        }
    }
}
