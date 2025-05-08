package com.video.vidbr.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserModel {
    private String id;
    private String email;
    private String username;
    private String profilePic;
    private String bio;
    private String name;
    private String birthday; // Campo para a data de nascimento
    private boolean verified; // Campo para verificar se o usuário é verificado
    private boolean verifiedgold; // Campo para verificar se o usuário é verificado como gold
    private List<String> followerList; // Lista de seguidores
    private List<String> followingList; // Lista de seguindo
    private String lastMessage; // Campo para a última mensagem
    private boolean isLastMessageReceived; // Campo para indicar se a última mensagem foi recebida
    private long lastMessageTimestamp; // Timestamp da última mensagem

    public UserModel() {
        // Construtor vazio necessário para Firebase Realtime Database
        this.verified = false;
        this.verifiedgold = false;
        this.birthday = "";
        this.lastMessage = "";
        this.isLastMessageReceived = false;
        this.lastMessageTimestamp = 0; // Inicializar o timestamp
        this.followerList = new ArrayList<>();
        this.followingList = new ArrayList<>();
    }

    public UserModel(String id, String email, String username) {
        this.id = id;
        this.email = email;
        this.name = "";
        this.username = username;
        this.profilePic = "";
        this.bio = "";
        this.birthday = "";
        this.verified = false;
        this.verifiedgold = false;
        this.followerList = new ArrayList<>();
        this.followingList = new ArrayList<>();
        this.lastMessage = "";
        this.isLastMessageReceived = false;
        this.lastMessageTimestamp = 0; // Inicializar o timestamp
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProfilePic() {
        return profilePic;
    }

    public void setProfilePic(String profilePic) {
        this.profilePic = profilePic;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getBirthday() {
        return birthday; // Getter para a data de nascimento
    }

    public void setBirthday(String birthday) { // Setter para a data de nascimento
        this.birthday = birthday;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isVerifiedGold() {
        return verifiedgold;
    }

    public void setVerifiedGold(boolean verifiedgold) {
        this.verifiedgold = verifiedgold;
    }

    public List<String> getFollowerList() {
        return followerList != null ? followerList : new ArrayList<>();
    }

    public void setFollowerList(List<String> followerList) {
        this.followerList = followerList != null ? followerList : new ArrayList<>();
    }

    public List<String> getFollowingList() {
        return followingList != null ? followingList : new ArrayList<>();
    }

    public void setFollowingList(List<String> followingList) {
        this.followingList = followingList != null ? followingList : new ArrayList<>();
    }

    public String getLastMessage() {
        return lastMessage; // Getter para a última mensagem
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage; // Setter para a última mensagem
    }

    public boolean isLastMessageReceived() {
        return isLastMessageReceived; // Getter para verificar se a última mensagem foi recebida
    }

    public void setLastMessageReceived(boolean lastMessageReceived) {
        isLastMessageReceived = lastMessageReceived; // Setter para definir se a última mensagem foi recebida
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp; // Getter para o timestamp da última mensagem
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp; // Setter para o timestamp da última mensagem
    }

    public String getLastMessageTime() {
        if (lastMessageTimestamp > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(new Date(lastMessageTimestamp)); // Retorna a hora formatada
        }
        return ""; // Retornar string vazia se não houver timestamp
    }

    public void deserializeLists(Map<String, Object> data) {
        if (data.containsKey("followerList")) {
            this.followerList = (List<String>) data.get("followerList");
        }
        if (data.containsKey("followingList")) {
            this.followingList = (List<String>) data.get("followingList");
        }
        if (data.containsKey("verified")) {
            this.verified = (boolean) data.get("verified");
        }
        if (data.containsKey("verifiedgold")) {
            this.verifiedgold = (boolean) data.get("verifiedgold");
        }
        if (data.containsKey("birthday")) {
            this.birthday = (String) data.get("birthday");
        }
        if (data.containsKey("lastMessage")) {
            this.lastMessage = (String) data.get("lastMessage");
        }
        if (data.containsKey("isLastMessageReceived")) {
            this.isLastMessageReceived = (boolean) data.get("isLastMessageReceived");
        }
        if (data.containsKey("lastMessageTimestamp")) {
            this.lastMessageTimestamp = (long) data.get("lastMessageTimestamp"); // Desserializar o timestamp
        }
    }
}
