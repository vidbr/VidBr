package com.video.vidbr;

import com.google.android.exoplayer2.SimpleExoPlayer;

public class VideoPlayerManager {
    private static volatile VideoPlayerManager instance;
    private SimpleExoPlayer player;

    private VideoPlayerManager() {
        // Private constructor to enforce singleton pattern
    }

    public static VideoPlayerManager getInstance() {
        if (instance == null) {
            synchronized (VideoPlayerManager.class) {
                if (instance == null) {
                    instance = new VideoPlayerManager();
                }
            }
        }
        return instance;
    }

    public void setPlayer(SimpleExoPlayer player) {
        this.player = player;
    }

    public SimpleExoPlayer getPlayer() {
        return player;
    }

    public void pauseVideo() {
        if (player != null /**&& player.isPlaying()**/) {
            player.setPlayWhenReady(false);
        }
    }

    public void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
