package com.jzk.motif_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jzk.motif_backend.model.Songs;
import com.jzk.motif_backend.repo.SongRepo;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SongService {

    private final SongRepo repo;

    public SongService(SongRepo repo) { this.repo = repo; }

    public Songs addSong(Map<String, Object> songData, int userId) throws JsonProcessingException {
        Songs song = new Songs();
        ObjectMapper objectMapper = new ObjectMapper();
        String songDataJson = objectMapper.writeValueAsString(songData);  // Convert to JSON string
        song.setSongData(songDataJson);
        song.setUserId(userId);
        return repo.save(song);
    }

    public String deleteSongsById(int songId) {
        repo.deleteById(songId);
        return "song deleted";
    }


    @Transactional
    public List<Map<String, Object>> getSongsByUserId(int userId) {
        List<Songs> songs = repo.findByUserId(userId);
        ObjectMapper mapper = new ObjectMapper();

        return songs.stream()
                .map(song -> {
                    try {
                        Map<String, Object> songMap = new HashMap<>();
                        songMap.put("songId", song.getSongId());
                        songMap.put("userId", song.getUserId());
                        songMap.put("songData", song.getSongDataAsObject(Map.class));

                        return songMap;
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(songData -> songData != null)
                .collect(Collectors.toList());
    }




}
