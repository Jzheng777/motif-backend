package com.jzk.motif_backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jzk.motif_backend.model.Songs;
import com.jzk.motif_backend.service.SongService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class SongController {
    private final SongService service;

    public SongController(SongService service) { this.service = service; }

    @PostMapping("/songs/addsong/{id}")
    public Songs addSong(@RequestBody Map<String, Object> songData, @PathVariable int id) throws JsonProcessingException {
        return service.addSong(songData, id);
    }

    @GetMapping("/songs/search/{id}")
    public List<Map<String, Object>> getSongsByUserId(@PathVariable int id) {
        return service.getSongsByUserId(id);
    }

    @DeleteMapping("/songs/delete/{id}")
    public String deleteSongsBySongId(@PathVariable int id) {
        return service.deleteSongsById(id);
    }

}
