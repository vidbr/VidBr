package com.video.vidbr.fragments;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.gms.ads.AdView;
import com.video.vidbr.R;
import com.video.vidbr.adapter.VideoListAdapter;
import com.video.vidbr.model.VideoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class ForYouFragment extends Fragment {

    private static final int PAGE_SIZE = 10;
    private static final String ARG_VIDEO_ID = "video_id";
    private VideoListAdapter adapter;
    private DocumentSnapshot lastVisible;
    private ViewPager2 viewPager;
    private boolean isLoading = false;
    private FirebaseFirestore firestore;
    private String videoId;
    private String userCountry;
    private List<Object> itemList = new ArrayList<>(); // List to hold videos and ads
    private TextView noVideos;
    private TextView publishPrompt;


    public static ForYouFragment newInstance(String videoId) {
        ForYouFragment fragment = new ForYouFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_ID, videoId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_for_you, container, false);

        viewPager = view.findViewById(R.id.for_you_view_pager);
        noVideos = view.findViewById(R.id.textViewNoVideos);
        publishPrompt = view.findViewById(R.id.textViewPublishPrompt);

        firestore = FirebaseFirestore.getInstance();

        // Get user's country
        Locale locale = Locale.getDefault();
        userCountry = locale.getDisplayCountry(Locale.ENGLISH);

        if (getArguments() != null) {
            videoId = getArguments().getString(ARG_VIDEO_ID);
        }

        fetchInitialData();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (!isLoading && position >= (adapter.getItemCount() - 1) * 0.8) {
                    fetchMoreData();
                }
            }
        });

        return view;
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(getContext().CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void fetchInitialData() {
        if (!isInternetAvailable()) {
            noVideos.setVisibility(View.VISIBLE);
            noVideos.setText(getString(R.string.no_internet_connection));
            publishPrompt.setVisibility(View.VISIBLE);
            publishPrompt.setText(getString(R.string.check_connection));
            return;
        }

        Query query = firestore.collection("videos")
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .whereEqualTo("country", userCountry)
                .whereEqualTo("visibility", "public")
                .limit(PAGE_SIZE);

        if (videoId != null) {
            query = query.whereEqualTo("videoId", videoId);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    VideoModel videoModel = document.toObject(VideoModel.class);
                    if (videoModel != null) {
                        itemList.add(videoModel);
                    }
                }
                insertAdsIntoList();
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                setupVideoList();
            } else {
                noVideos.setVisibility(View.VISIBLE);
                publishPrompt.setVisibility(View.VISIBLE);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Error fetching data", Toast.LENGTH_SHORT).show();
            Log.e("ErrorFD", "Error fetching data", e);
        });
    }

    private void setupVideoList() {
        adapter = new VideoListAdapter(getContext(), itemList);
        viewPager.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void fetchMoreData() {
        if (lastVisible == null || isLoading) return;

        isLoading = true;

        Query query = firestore.collection("videos")
                .orderBy("createdTime", Query.Direction.DESCENDING)
                .whereEqualTo("country", userCountry)
                .whereEqualTo("visibility", "public")
                .startAfter(lastVisible)
                .limit(PAGE_SIZE);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!queryDocumentSnapshots.isEmpty()) {
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    VideoModel videoModel = document.toObject(VideoModel.class);
                    if (videoModel != null) {
                        itemList.add(videoModel);
                    }
                }
                insertAdsIntoList();
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                adapter.notifyDataSetChanged();
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            isLoading = false;
            Toast.makeText(getContext(), "Error fetching more data", Toast.LENGTH_SHORT).show();
        });
    }

    private void insertAdsIntoList() {
        for (int i = 10; i < itemList.size(); i += 11) {
            AdView adView = new AdView(getContext());
            adView.setAdSize(com.google.android.gms.ads.AdSize.MEDIUM_RECTANGLE);
            adView.setAdUnitId("ca-app-pub-3940256099942544/9214589741"); // Coloque seu ID de unidade de anúncio aqui
            itemList.add(i, adView);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (viewPager != null) {
            viewPager.setAdapter(null);
        }

        itemList.clear();
    }
}
