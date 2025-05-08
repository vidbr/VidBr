package com.video.vidbr.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.MessageModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private Context mContext;
    private List<MessageModel> mMessageList;
    private String mCurrentUserId;
    private OnMessageLongClickListener onMessageLongClickListener;
    private long lastTimestamp; // Timestamp da última mensagem

    public interface OnMessageLongClickListener {
        void onMessageLongClick(MessageModel message);
    }

    public MessageAdapter(Context mContext, List<MessageModel> mMessageList, String mCurrentUserId, OnMessageLongClickListener onMessageLongClickListener) {
        this.mContext = mContext;
        this.mMessageList = mMessageList;
        this.mCurrentUserId = mCurrentUserId;
        this.onMessageLongClickListener = onMessageLongClickListener;
        this.lastTimestamp = 0; // Inicializa o último timestamp
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        MessageModel message = mMessageList.get(position);

        // Verifica se a data da mensagem é diferente da última mensagem
        boolean showDate = shouldShowDate(message.getTimestamp(), position);
        if (showDate) {
            holder.textViewDate.setText(formatDate(message.getTimestamp()));
            holder.textViewDate.setVisibility(View.VISIBLE);
        } else {
            holder.textViewDate.setVisibility(View.GONE);
        }

        // Reset profile image to default
        holder.imageViewProfile.setImageResource(R.drawable.icon_account_circle);

        if (message.getSenderId().equals(mCurrentUserId)) {
            // Mensagem enviada
            holder.textViewSentMessage.setText(message.getMessage());
            holder.textViewSentMessage.setVisibility(View.VISIBLE);
            holder.textViewReceivedMessage.setVisibility(View.GONE);
            holder.imageViewProfile.setVisibility(View.GONE); // Ocultar imagem de perfil para mensagens enviadas

            // Definir horário da mensagem enviada
            holder.textViewTimestampSent.setText(formatTimestamp(message.getTimestamp()));
            holder.textViewTimestampSent.setVisibility(View.VISIBLE);
            holder.textViewTimestampReceived.setVisibility(View.GONE); // Certifique-se de que o timestamp recebido não seja exibido
        } else {
            // Mensagem recebida
            holder.textViewReceivedMessage.setText(message.getMessage());
            holder.textViewReceivedMessage.setVisibility(View.VISIBLE);
            holder.textViewSentMessage.setVisibility(View.GONE);
            holder.imageViewProfile.setVisibility(View.VISIBLE); // Mostrar imagem de perfil para mensagens recebidas

            // Carregar imagem de perfil
            if (message.getProfileImageUrl() != null && !message.getProfileImageUrl().isEmpty()) {
                loadImageWithGlide(holder.imageViewProfile, message.getProfileImageUrl());
            } else {
                FirebaseFirestore.getInstance().collection("users")
                        .document(message.getSenderId())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String imageUrl = documentSnapshot.getString("profilePic");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    loadImageWithGlide(holder.imageViewProfile, imageUrl);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            // Você pode logar o erro ou manter o ícone padrão
                        });
            }

            // Definir horário da mensagem recebida
            holder.textViewTimestampReceived.setText(formatTimestamp(message.getTimestamp()));
            holder.textViewTimestampReceived.setVisibility(View.VISIBLE);
            holder.textViewTimestampSent.setVisibility(View.GONE); // Certifique-se de que o timestamp enviado não seja exibido
        }

        // Long click listener
        holder.textViewSentMessage.setOnLongClickListener(v -> {
            onMessageLongClickListener.onMessageLongClick(message);
            return true;
        });

        // OnClickListener para o ícone de perfil
        holder.imageViewProfile.setOnClickListener(v -> {
            Intent intent = new Intent(mContext, ProfileActivity.class);
            intent.putExtra("profile_user_id", message.getSenderId()); // Passando o ID do usuário
            mContext.startActivity(intent);
        });

        // Atualiza o timestamp da última mensagem
        lastTimestamp = message.getTimestamp();
    }

    private void loadImageWithGlide(ImageView imageView, String imageUrl) {
        Glide.with(mContext)
                .load(imageUrl)
                .placeholder(R.drawable.icon_account_circle) // Placeholder while loading
                .error(R.drawable.icon_account_circle) // Error image
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView);
    }

    private boolean shouldShowDate(long timestamp, int position) {
        // Exibir a data se for a primeira mensagem ou se a data da mensagem atual for diferente da mensagem anterior
        if (position == 0) {
            return true; // Sempre mostra a data para a primeira mensagem
        }

        MessageModel previousMessage = mMessageList.get(position - 1);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String previousDate = sdf.format(new Date(previousMessage.getTimestamp()));
        String currentDate = sdf.format(new Date(timestamp));
        return !previousDate.equals(currentDate);
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        public TextView textViewSentMessage;
        public TextView textViewReceivedMessage;
        public TextView textViewTimestampSent;
        public TextView textViewTimestampReceived;
        public TextView textViewDate; // Adicionado para a data da mensagem
        public ImageView imageViewProfile;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewSentMessage = itemView.findViewById(R.id.text_view_sent_message);
            textViewReceivedMessage = itemView.findViewById(R.id.text_view_received_message);
            textViewTimestampSent = itemView.findViewById(R.id.text_view_timestamp_sent);
            textViewTimestampReceived = itemView.findViewById(R.id.text_view_timestamp_received);
            textViewDate = itemView.findViewById(R.id.text_view_date); // Adicionado para a data da mensagem
            imageViewProfile = itemView.findViewById(R.id.image_view_profile);
        }
    }
}
