package com.jzk.motif_backend.dto;

public class UserDTO {
    private int id;
    private String username;
    private String profile_pic;
    private String spotify;

    public UserDTO(int id, String username, String profile_pic, String spotify) {
        this.id = id;
        this.username = username;
        this.profile_pic = profile_pic;
        this.spotify = spotify;
    }

    public String getSpotify() {
        return spotify;
    }

    public void setSpotify(String spotify) {
        this.spotify = spotify;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfile_pic() {
        return profile_pic;
    }

    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }
}
