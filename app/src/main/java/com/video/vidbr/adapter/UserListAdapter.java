package com.video.vidbr.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.databinding.ItemUserBinding;
import com.video.vidbr.model.UserModel;

public class UserListAdapter extends FirestoreRecyclerAdapter<UserModel, UserListAdapter.UserViewHolder> {

    private final String currentUserId;
    private final boolean isUserLoggedIn;

    public UserListAdapter(@NonNull FirestoreRecyclerOptions<UserModel> options) {
        super(options);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        isUserLoggedIn = currentUserId != null;
    }

    @Override
    protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull UserModel model) {
        holder.bind(model);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ItemUserBinding binding = ItemUserBinding.inflate(inflater, parent, false);
        return new UserViewHolder(binding);
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {

        private final ItemUserBinding binding;

        public UserViewHolder(@NonNull ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(UserModel user) {
            binding.setUser(user);
            binding.name.setText(user.getName());
            binding.username.setText(user.getUsername());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("videos")
                    .whereEqualTo("uploaderId", user.getId())
                    .whereEqualTo("visibility", "public")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int videoCount = querySnapshot.size();
                        // Formatar a contagem de seguidores e seguir utilizando o formatLikesCount
                        binding.followersCount.setText(
                                formatLikesCount(user.getFollowerList().size()) + " " +
                                        binding.getRoot().getContext().getString(R.string.seguidores) + " • " +
                                        videoCount + " " +
                                        binding.getRoot().getContext().getString(R.string.videos)
                        );
                    });

            binding.verifiedIcon.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
            binding.verifiedGold.setVisibility(user.isVerifiedGold() ? View.VISIBLE : View.GONE);

            Glide.with(binding.profileImage.getContext())
                    .load(user.getProfilePic())
                    .circleCrop()
                    .placeholder(R.drawable.icon_account_circle)
                    .into(binding.profileImage);

            updateFollowButton(user);

            binding.followButton.setOnClickListener(v -> {
                if (isUserLoggedIn) {
                    handleFollowButtonClick(user);
                } else {
                    // Handle guest user interaction
                    // e.g., show a message or redirect to login
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("profile_user_id", user.getId());
                v.getContext().startActivity(intent);
            });

            if (user.getId().equals(currentUserId)) {
                binding.followButton.setVisibility(View.GONE);
            } else {
                binding.followButton.setVisibility(View.VISIBLE);
            }
        }

        private void updateFollowButton(UserModel user) {
            if (isUserLoggedIn) {
                if (user.getFollowerList().contains(currentUserId)) {
                    binding.followButton.setText(binding.getRoot().getContext().getString(R.string.unfollow));
                } else {
                    binding.followButton.setText(binding.getRoot().getContext().getString(R.string.follow));
                }
            } else {
                binding.followButton.setText(binding.getRoot().getContext().getString(R.string.follow)); // Faça login para seguir
                binding.followButton.setEnabled(false); // Disable button for guests
            }
        }


        private void handleFollowButtonClick(UserModel user) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            db.collection("users").document(currentUserId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        UserModel currentUser = documentSnapshot.toObject(UserModel.class);

                        if (user.getFollowerList().contains(currentUserId)) {
                            // Unfollow
                            user.getFollowerList().remove(currentUserId);
                            currentUser.getFollowingList().remove(user.getId());
                        } else {
                            // Follow
                            user.getFollowerList().add(currentUserId);
                            currentUser.getFollowingList().add(user.getId());
                        }

                        // Update user in Firestore
                        db.collection("users").document(user.getId()).set(user);
                        db.collection("users").document(currentUserId).set(currentUser);

                        updateFollowButton(user);
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
}
