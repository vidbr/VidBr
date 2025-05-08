package com.video.vidbr.adapter;

// Importações
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
import com.bumptech.glide.request.RequestOptions;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.UserModel;

import java.util.List;

public class LikedUsersAdapter extends RecyclerView.Adapter<LikedUsersAdapter.UserViewHolder> {
    private List<UserModel> userList;
    private Context context;

    public LikedUsersAdapter(Context context, List<UserModel> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_liked, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserModel user = userList.get(position);
        holder.username.setText(user.getUsername());
        holder.name.setText(user.getName());

        // Carrega a imagem do perfil (se a URL estiver disponível)
        if (user.getProfilePic() != null && !user.getProfilePic().isEmpty()) {
            Glide.with(context)
                    .load(user.getProfilePic()) // Usando Glide para carregar a imagem
                    .circleCrop()
                    .into(holder.profilePic);
        }

        // Defina o OnClickListener para abrir o perfil do usuário
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("profile_user_id", user.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView username;
        TextView name;

        public UserViewHolder(View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.profilePic);
            username = itemView.findViewById(R.id.username);
            name = itemView.findViewById(R.id.name);
        }
    }
}

