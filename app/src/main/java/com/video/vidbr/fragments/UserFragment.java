package com.video.vidbr.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.video.vidbr.ProfileActivity;
import com.video.vidbr.R;
import com.video.vidbr.model.UserModel;

import java.util.HashMap;
import java.util.Map;

public class UserFragment extends Fragment {

    private RecyclerView recyclerView;
    private FirebaseFirestore firestore;
    private FirestoreRecyclerAdapter<UserModel, UserViewHolder> adapter;
    private String searchQuery = "";
    private LinearLayout noResultsLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user, container, false);

        recyclerView = view.findViewById(R.id.user_recycler_view);
        noResultsLayout = view.findViewById(R.id.no_results_layout);

        firestore = FirebaseFirestore.getInstance();

        setupRecyclerView(searchQuery);
        return view;
    }

    public void searchUsers(String query) {
        searchQuery = query;
        if (adapter != null) {
            adapter.stopListening();
        }
        setupRecyclerView(searchQuery);
        if (adapter != null) {
            adapter.startListening();
        }
    }

    private void setupRecyclerView(String query) {
        if (getContext() == null) {
            return;
        }

        Query baseQuery;
        if (!query.isEmpty()) {

            baseQuery = firestore.collection("users")
                    .orderBy("username")
                    .startAt(query)
                    .endAt(query + "\uf8ff")
                    .limit(10); // Set the limit here (e.g., 20 users)
        } else {

            baseQuery = firestore.collection("users")
                    .whereEqualTo("username", "__no_match__")
                    .limit(10); // Set the limit here for no match
        }

        FirestoreRecyclerOptions<UserModel> options = new FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(baseQuery, UserModel.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<UserModel, UserViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull UserViewHolder holder, int position, @NonNull UserModel model) {
                holder.bind(model);
            }

            @NonNull
            @Override
            public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
                return new UserViewHolder(view);
            }

            @Override
            public void onDataChanged() {
                super.onDataChanged();

                if (!searchQuery.isEmpty()) {
                    if (getItemCount() > 0) {
                        noResultsLayout.setVisibility(View.GONE);
                    } else {
                        noResultsLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    noResultsLayout.setVisibility(View.GONE);
                }
            }
        };

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView nameTextView;
        TextView usernameTextView;
        TextView followersCountTextView;
        Button followButton;
        ImageView verifiedIcon;
        ImageView verifiedGold;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.profile_image);
            nameTextView = itemView.findViewById(R.id.name);
            usernameTextView = itemView.findViewById(R.id.username);
            followersCountTextView = itemView.findViewById(R.id.followers_count);
            followButton = itemView.findViewById(R.id.follow_button);
            verifiedIcon = itemView.findViewById(R.id.verified_icon);
            verifiedGold = itemView.findViewById(R.id.verifiedGold);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    UserModel user = adapter.getItem(position);
                    if (user != null) {
                        Intent intent = new Intent(itemView.getContext(), ProfileActivity.class);
                        intent.putExtra("profile_user_id", user.getId());
                        itemView.getContext().startActivity(intent);
                    }
                }
            });
        }

        public void bind(UserModel user) {
            nameTextView.setText(user.getName());
            usernameTextView.setText(user.getUsername());

            verifiedIcon.setVisibility(user.isVerified() ? View.VISIBLE : View.GONE);
            verifiedGold.setVisibility(user.isVerifiedGold() ? View.VISIBLE : View.GONE);

            Glide.with(profileImageView).load(user.getProfilePic())
                    .circleCrop()
                    .placeholder(R.drawable.icon_account_circle)
                    .into(profileImageView);

            firestore.collection("videos")
                    .whereEqualTo("uploaderId", user.getId())
                    .whereEqualTo("visibility", "public")
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null) {
                            int videoCount = queryDocumentSnapshots.size();
                            int followersCount = user.getFollowerList().size();
                            String followersCountText = formatLikesCount(followersCount) + " " + getContext().getString(R.string.seguidores) + " • " + formatLikesCount(videoCount) + " " + getContext().getString(R.string.videos);
                            followersCountTextView.setText(followersCountText);
                        }
                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                    });

            updateFollowButton(user);

            followButton.setOnClickListener(v -> {
                // Follow button functionality removed for anonymous users
                Toast.makeText(itemView.getContext(), "Login to follow users", Toast.LENGTH_SHORT).show();
            });
        }

        private void updateFollowButton(UserModel user) {
            // Follow button state logic removed for anonymous users
            followButton.setVisibility(View.GONE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.stopListening();
            adapter = null; // Nullify the reference to allow garbage collection.
        }
        recyclerView.setAdapter(null); // Clear adapter from RecyclerView to avoid leaks.
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
}
