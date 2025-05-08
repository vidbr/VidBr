package com.video.vidbr.model;

import java.util.List;

public class HashtagModel {
    private String id;
    private String name;
    private List<String> relatedVideos;

    public HashtagModel() {
    }

    public HashtagModel(String id, String name, List<String> relatedVideos) {
        this.id = id;
        this.name = name;
        this.relatedVideos = relatedVideos;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getRelatedVideos() {
        return relatedVideos;
    }

    public void setRelatedVideos(List<String> relatedVideos) {
        this.relatedVideos = relatedVideos;
    }
}
