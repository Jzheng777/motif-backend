package com.jzk.motif_backend.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Entity
public class Songs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int songId;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String songData;

    @Column(name = "user_id", nullable = false)
    private int userId;


    public int getSongId() { return songId; }
    public void setSongId(int songId) { this.songId = songId; }
    public String getSongData() { return songData; }
    public void setSongData(String songData) { this.songData = songData; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public void setSongData(Object songObject) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        this.songData = objectMapper.writeValueAsString(songObject);
    }

    public <T> T getSongDataAsObject(Class<T> type) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(this.songData, type);
    }
}
