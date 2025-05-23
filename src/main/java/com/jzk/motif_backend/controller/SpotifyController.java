package com.jzk.motif_backend.controller;

import com.jzk.motif_backend.service.SpotifyService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class SpotifyController {

    private final SpotifyService service;
    public SpotifyController(SpotifyService service) { this.service = service; }

    @GetMapping("/search/genres")
    public List<List<Map<String, Object>>> searchGenres(@RequestParam String query) {
        List<Map<String, Object>> artistResults = service.searchArtists(query);
        List<Map<String, Object>> trackResults = service.searchTracks(query);
        List<List<Map<String, Object>>> combinedResults = new ArrayList<>();
        combinedResults.add(artistResults);
        combinedResults.add(trackResults);
        return combinedResults;
    }

//    @GetMapping("/spotify/recommendations")
//    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@RequestParam("trackUri") String trackUri) {
//        try {
//            List<Map<String, Object>> recommendations = service.getRecommendations(trackUri);
//            return ResponseEntity.ok(recommendations);
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body(null);
//        }
//    }

    @GetMapping("/spotify/recommendations")
    public ResponseEntity<List<Map<String, Object>>> getRecommendations(@RequestParam("trackUri") String trackId) {
        try {
            List<Map<String, Object>> recommendations = service.getRecommendations(trackId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            System.err.println("Error in /spotify/recommendations: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }




    @GetMapping("/search/artists")
    public List<Map<String, Object>> searchArtists(@RequestParam String query) {
        return service.searchArtists(query);
    }

    @GetMapping("/search/tracks")
    public List<Map<String, Object>> searchTracks(@RequestParam String query) {
        return service.searchTracks(query);
    }

    @GetMapping("/search/artisttracks")
    public List<Map<String, Object>> searchArtistsByTracks(@RequestParam String query) {
        return service.searchTracksByArtist(query);
    }

    @GetMapping("/spotify/top10songs")
    public List<Map<String, Object>> getTop10Songs(@RequestParam("playlistId") String playlistId) {
        return service.getTop10Songs(playlistId);
    }

    @GetMapping("/spotify/top10artists")
    public List<Map<String, Object>> getTop10Artists(@RequestParam("playlistId") String playlistId) {
        return service.getTop10Artists(playlistId);
    }

}


//    @GetMapping("/spotify/login")
//    public ResponseEntity<Void> initiateLogin(@RequestParam("userId") String userId) {
//        URI spotifyAuthUrl = service.getSpotifyLoginUrl(userId);
//        return ResponseEntity.status(HttpStatus.FOUND).location(spotifyAuthUrl).build();
//    }

//    @GetMapping("/spotify/callback")
//    public ResponseEntity<Void> handleCallback(
//            @RequestParam("code") String code,
//            @RequestParam("state") String userId,
//            HttpServletResponse response) {
//        try {
//            String refreshToken = service.exchangeAuthorizationCodeForTokens(code, userId);
//
//            // Redirect to frontend
//            String redirectUrl = "http://localhost:3000/saved";
//            response.sendRedirect(redirectUrl);
//            return ResponseEntity.status(HttpStatus.FOUND).build();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
//        }
//    }


//    @GetMapping("/spotify/playlists")
//    public ResponseEntity<List<Map<String, Object>>> getUserPlaylists(@RequestParam("userId") String userId) {
//        try {
//            List<Map<String, Object>> playlists = service.getUserPlaylists(userId);
//            return ResponseEntity.ok(playlists);
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
//        }
//    }