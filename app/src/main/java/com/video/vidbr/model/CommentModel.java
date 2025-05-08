package com.video.vidbr.model;

import java.util.Date;

public class CommentModel {
    private String id; // Adicionando campo para o ID do comentário
    private String userId;
    private String username;
    private String commentText;
    private Date commentDate; // Campo para armazenar a data exata do comentário
    private long timestamp;
    private String profilePic; // Field to store profile picture URL

    public CommentModel() {
        // Este construtor vazio é necessário para a desserialização do Firestore
    }

    // Constructor
    public CommentModel(String id, String userId, String username, String profilePic, String commentText, Date commentDate, long timestamp) {
        this.id = id; // Inicializa o ID
        this.userId = userId;
        this.username = username;
        this.commentText = commentText;
        this.commentDate = commentDate;
        this.timestamp = timestamp;
        this.profilePic = profilePic;
    }

    // Getters and setters
    public String getId() {
        return id; // Método para obter o ID do comentário
    }

    public void setId(String id) {
        this.id = id; // Método para definir o ID do comentário
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Date getCommentDate() {
        return commentDate;
    }

    public void setCommentDate(Date commentDate) {
        this.commentDate = commentDate;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
