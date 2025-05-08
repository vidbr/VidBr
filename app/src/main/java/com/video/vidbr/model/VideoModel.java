package com.video.vidbr.model;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VideoModel {
    private String videoId;
    private String title;
    private String url;
    private String uploaderId;
    private Timestamp createdTime;
    private int likesCount;
    private List<String> likedBy;
    private boolean isPlaying;
    private int commentCount;
    private List<String> hashtags;
    private String country;
    private String visibility;

    public VideoModel() {
        this.visibility = "private"; // Default visibility
    }

    public VideoModel(String videoId) {
        this.videoId = videoId;
        this.title = "";
        this.url = "";
        this.uploaderId = "";
        this.createdTime = new Timestamp(new Date());
        this.likesCount = 0;
        this.likedBy = new ArrayList<>();
        this.isPlaying = false;
        this.commentCount = 0;
        this.hashtags = new ArrayList<>();
        this.country = "";
        this.visibility = "private";
    }

    public VideoModel(String videoId, String title, String url, String uploaderId, Timestamp createdTime,
                      int likesCount, List<String> likedBy, boolean isPlaying, int commentCount,
                      List<String> hashtags, String country, String visibility) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
        this.uploaderId = uploaderId;
        this.createdTime = createdTime;
        this.likesCount = likesCount;
        this.likedBy = likedBy;
        this.isPlaying = isPlaying;
        this.commentCount = commentCount;
        this.hashtags = hashtags;
        this.country = country;
        this.visibility = visibility != null ? visibility : "private";
    }

    // Getters and Setters
    public String getVideoId() {
        return videoId;
    }

    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be null or empty");
        }
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        this.url = url;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public Timestamp getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Timestamp createdTime) {
        this.createdTime = createdTime;
    }

    public int getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }

    public List<String> getLikedBy() {
        return likedBy;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public List<String> getHashtags() {
        return hashtags;
    }

    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getVisibility() {
        return visibility;
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility != null ? visibility : "private";
    }

    public String getId() {
        return videoId;
    }

    public String getVideoUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "VideoModel{" +
                "videoId='" + videoId + '\'' +
                ", title='" + title + '\'' +
                ", url='" + url + '\'' +
                ", uploaderId='" + uploaderId + '\'' +
                ", createdTime=" + createdTime +
                ", likesCount=" + likesCount +
                ", commentCount=" + commentCount +
                ", visibility='" + visibility + '\'' +
                '}';
    }
}
