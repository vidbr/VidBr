package com.video.vidbr.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.video.vidbr.fragments.FollowingFragment;
import com.video.vidbr.fragments.ForYouFragment;

public class TabFragmentAdapter extends FragmentStateAdapter {

    public TabFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new FollowingFragment();
            case 1:
                return new ForYouFragment();
            default:
                return new ForYouFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
