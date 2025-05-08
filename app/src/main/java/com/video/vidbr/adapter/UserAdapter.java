package com.video.vidbr.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.video.vidbr.ChatActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.UserModel;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private Context context;
    private List<UserModel> userList;

    public UserAdapter(Context context, List<UserModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_chat, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.userName.setText(user.getName());

        String lastMessage = user.getLastMessage();
        holder.lastMessageTextView.setText(lastMessage.isEmpty() ? "No messages" : lastMessage);

        // Definir o estilo e a cor do texto da última mensagem com base na recepção
        if (!lastMessage.isEmpty()) {
            if (user.isLastMessageReceived()) {
                holder.lastMessageTextView.setTypeface(null, Typeface.BOLD);
                holder.lastMessageTextView.setTextColor(Color.BLACK);
            } else {
                holder.lastMessageTextView.setTypeface(null, Typeface.NORMAL);
                holder.lastMessageTextView.setTextColor(Color.GRAY);
            }
        } else {
            holder.lastMessageTextView.setTypeface(null, Typeface.NORMAL);
            holder.lastMessageTextView.setTextColor(Color.GRAY);
        }

        // Carregar a imagem de perfil usando Glide
        Glide.with(context)
                .load(user.getProfilePic())
                .placeholder(R.drawable.icon_account_circle)
                .error(R.drawable.icon_account_circle)
                .circleCrop()
                .into(holder.profileImage);

        // Definir a hora da última mensagem usando o método getLastMessageTime()
        String lastMessageTime = user.getLastMessageTime();
        holder.lastMessageTimeTextView.setText(lastMessageTime.isEmpty() ? "No time" : lastMessageTime);

        // Set the click listener for the item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("chatUserId", user.getId());
            intent.putExtra("chatUserName", user.getUsername());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userName;
        ImageView profileImage;
        TextView lastMessageTextView; // Novo TextView para a última mensagem
        TextView lastMessageTimeTextView; // TextView para a hora da última mensagem

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.text_view_user_name);
            profileImage = itemView.findViewById(R.id.image_view_profile);
            lastMessageTextView = itemView.findViewById(R.id.text_view_last_message); // Inicializar lastMessageTextView
            lastMessageTimeTextView = itemView.findViewById(R.id.text_view_last_message_time); // Inicializar lastMessageTimeTextView
        }
    }
}
