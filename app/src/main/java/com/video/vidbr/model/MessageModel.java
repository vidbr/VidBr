package com.video.vidbr.model;

public class MessageModel {
    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp; // Alterado para long
    private String chatId;
    private String profileImageUrl; // URL da foto de perfil

    public MessageModel() {
        // Construtor vazio necessário para Firebase Realtime Database
    }

    public MessageModel(String messageId, String senderId, String receiverId, String message, long timestamp, String chatId) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.chatId = chatId;
    }

    // Getters e Setters

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() { // Alterado para long
        return timestamp;
    }

    public void setTimestamp(long timestamp) { // Alterado para long
        this.timestamp = timestamp;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
