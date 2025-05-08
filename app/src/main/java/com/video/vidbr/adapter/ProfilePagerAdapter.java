package com.video.vidbr.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.video.vidbr.fragments.UserVideosFragment;
import com.video.vidbr.fragments.LikedVideosFragment;

public class ProfilePagerAdapter extends FragmentStateAdapter {

    public ProfilePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new UserVideosFragment();
            case 1:
                return new LikedVideosFragment();
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return 2; // We have two tabs
    }
}
