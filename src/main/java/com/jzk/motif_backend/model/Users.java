package com.jzk.motif_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String username;
    private String password;
    private String profile_pic;
    private String spotify;
    @Column(name = "playlists", columnDefinition = "text[]")
    @ElementCollection
    private List<String> playlists;

    public String getSpotify() { return spotify;}
    public void setSpotify(String spotify) { this.spotify = spotify;}
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
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getProfile_pic() {
        return profile_pic;
    }
    public void setProfile_pic(String profile_pic) {
        this.profile_pic = profile_pic;
    }
    public List<String> getPlaylists() { return playlists; }

    public void setPlaylists(List<String> playlists) { this.playlists = playlists; }

    @Override
    public String toString() {
        return "Users{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", profile_pic='" + profile_pic + '\'' +
                ", spotify='" + spotify + '\'' +
                ", playlists=" + playlists +
                '}';
    }
}
