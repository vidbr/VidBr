package com.video.vidbr.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.video.vidbr.HashtagVideosActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.HashtagModel;

import java.util.List;

public class HashtagListAdapter extends RecyclerView.Adapter<HashtagListAdapter.HashtagViewHolder> {

    private Context context;
    private List<HashtagModel> hashtags;

    public HashtagListAdapter(Context context, List<HashtagModel> hashtags) {
        this.context = context;
        this.hashtags = hashtags;
    }

    @NonNull
    @Override
    public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hashtag, parent, false);
        return new HashtagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
        HashtagModel hashtag = hashtags.get(position);
        holder.hashtagTextView.setText("#" + hashtag.getName());

        // Adiciona o listener de clique no item da hashtag
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, HashtagVideosActivity.class);
            intent.putExtra("hashtag", hashtag.getName()); // Passa o nome da hashtag
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return hashtags.size();
    }


    public static class HashtagViewHolder extends RecyclerView.ViewHolder {
        TextView hashtagTextView;

        public HashtagViewHolder(@NonNull View itemView) {
            super(itemView);
            hashtagTextView = itemView.findViewById(R.id.hashtag_text);
        }
    }

}
