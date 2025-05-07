package com.jzk.motif_backend.service;
import java.util.concurrent.*;
import com.jzk.motif_backend.model.Users;
import com.jzk.motif_backend.repo.UserRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class SpotifyService {

    private final RestTemplate restTemplate;
    private final UserRepo repo;

    @Value("${spotify.client-id}")
    private String clientId;
    @Value("AQCuamnAUlx_epAlxcAlNbK0gT8UYPaYNY2vRS_1vn7xeMspfeb4hEoRe2zhsqlSW-FlkrQbzKjHlG2V-MKdprRDJ76BpVqx-_AJKMo5D3JVIo2ZK9yLZZsZVFI3xXAomsk")
    private String refreshToken;
    @Value("${spotify.client-secret}")
    private String clientSecret;
    @Value("${spotify.redirect-uri}")
    private String redirectUri;
    @Value("${spotify.scopes}")
    private String scopes;
    private String accessToken;
    private long accessTokenExpiry = 0;
    @Value("${lastfm.api.key}")
    private String lastFmApiKey;

    private static final String SPOTIFY_API_BASE_URL = "https://api.spotify.com/v1/search";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_PLAYLIST_URL = "https://api.spotify.com/v1/playlists/";

    public SpotifyService(RestTemplate restTemplate, UserRepo repo) {
        this.restTemplate = restTemplate;
        this.repo = repo;
    }


    private String getAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        String authValue = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + authValue);
        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                SPOTIFY_TOKEN_URL,
                HttpMethod.POST,
                request,
                Map.class
        );
        return (String) response.getBody().get("access_token");
    }

    private String extractTrackId(String uri) {
        if (uri.contains("spotify:track:")) {
            return uri.split(":")[2];
        } else if (uri.contains("spotify.com/track/")) {
            return uri.substring(uri.lastIndexOf("/") + 1).split("\\?")[0];
        }
        return uri;
    }

    public List<Map<String, Object>> getRecommendations(String trackUri) throws Exception {
        String accessToken = getAccessToken();
        String trackId = extractTrackId(trackUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ObjectMapper mapper = new ObjectMapper();

        // STEP 1: Get track details
        String trackUrl = "https://api.spotify.com/v1/tracks/" + trackId;
        ResponseEntity<String> response = restTemplate.exchange(trackUrl, HttpMethod.GET, entity, String.class);
        JsonNode trackJson = mapper.readTree(response.getBody());
        String trackName = trackJson.get("name").asText();
        String artistName = trackJson.get("artists").get(0).get("name").asText();

        // STEP 2: Get similar tracks from Last.fm
//        String lastFmApiKey = "dfbe04e0f67463a3c1f0474235bfdc12";
        String lastFmTrackUrl = UriComponentsBuilder.fromHttpUrl("http://ws.audioscrobbler.com/2.0/")
                .queryParam("method", "track.getsimilar")
                .queryParam("artist", artistName)
                .queryParam("track", trackName)
                .queryParam("api_key", lastFmApiKey)
                .queryParam("format", "json")
                .queryParam("limit", 5)
                .toUriString();

        ResponseEntity<String> lastFmResponse = restTemplate.getForEntity(lastFmTrackUrl, String.class);
        JsonNode similarTracks = mapper.readTree(lastFmResponse.getBody()).path("similartracks").path("track");

        List<Map<String, Object>> recommendations = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<Map<String, Object>>> futures = new ArrayList<>();

        if (similarTracks.isArray() && similarTracks.size() > 0) {
            for (JsonNode similar : similarTracks) {
                futures.add(executor.submit(() -> {
                    String similarName = similar.get("name").asText();
                    String similarArtist = similar.get("artist").get("name").asText();
                    String query = URLEncoder.encode(similarName + " " + similarArtist, StandardCharsets.UTF_8);
                    String searchUrl = SPOTIFY_API_BASE_URL + "?q=" + query + "&type=track&limit=1";

                    try {
                        ResponseEntity<String> searchResponse = restTemplate.exchange(searchUrl, HttpMethod.GET, entity, String.class);
                        JsonNode items = mapper.readTree(searchResponse.getBody()).path("tracks").path("items");
                        if (items.isArray() && items.size() > 0) {
                            return convertSpotifyTrackToMap(items.get(0));
                        }
                    } catch (HttpClientErrorException ignored) {}
                    return null;
                }));
            }

            for (Future<Map<String, Object>> future : futures) {
                try {
                    Map<String, Object> result = future.get();
                    if (result != null) recommendations.add(result);
                } catch (Exception ignored) {}
            }
        }

        // STEP 3: Fallback to similar artists
        if (recommendations.isEmpty()) {
            String similarArtistsUrl = UriComponentsBuilder.fromHttpUrl("http://ws.audioscrobbler.com/2.0/")
                    .queryParam("method", "artist.getsimilar")
                    .queryParam("artist", URLEncoder.encode(artistName, StandardCharsets.UTF_8))
                    .queryParam("api_key", lastFmApiKey)
                    .queryParam("format", "json")
                    .queryParam("limit", 5)
                    .toUriString();

            ResponseEntity<String> similarArtistsResp = restTemplate.getForEntity(similarArtistsUrl, String.class);
            JsonNode similarArtists = mapper.readTree(similarArtistsResp.getBody()).path("similarartists").path("artist");

            List<Future<List<JsonNode>>> artistFutures = new ArrayList<>();

            for (JsonNode artist : similarArtists) {
                artistFutures.add(executor.submit(() -> {
                    List<JsonNode> topTracks = new ArrayList<>();
                    String similarArtistName = artist.get("name").asText();
                    String query = URLEncoder.encode("artist:" + similarArtistName, StandardCharsets.UTF_8);
                    String artistSearchUrl = "https://api.spotify.com/v1/search?q=" + query + "&type=artist&limit=1";

                    try {
                        ResponseEntity<String> artistResp = restTemplate.exchange(artistSearchUrl, HttpMethod.GET, entity, String.class);
                        JsonNode artistItems = mapper.readTree(artistResp.getBody()).path("artists").path("items");
                        if (artistItems.isArray() && artistItems.size() > 0) {
                            String artistId = artistItems.get(0).get("id").asText();
                            String topTracksUrl = "https://api.spotify.com/v1/artists/" + artistId + "/top-tracks?market=US";

                            ResponseEntity<String> topTracksResp = restTemplate.exchange(topTracksUrl, HttpMethod.GET, entity, String.class);
                            JsonNode tracks = mapper.readTree(topTracksResp.getBody()).path("tracks");

                            for (JsonNode topTrack : tracks) {
                                topTracks.add(topTrack);
                            }
                        }
                    } catch (HttpClientErrorException ignored) {}

                    return topTracks;
                }));
            }

            List<JsonNode> collectedTracks = new ArrayList<>();
            for (Future<List<JsonNode>> future : artistFutures) {
                try {
                    collectedTracks.addAll(future.get());
                } catch (Exception ignored) {}
            }

            Collections.shuffle(collectedTracks);
            for (int i = 0; i < Math.min(5, collectedTracks.size()); i++) {
                recommendations.add(convertSpotifyTrackToMap(collectedTracks.get(i)));
            }
        }

        executor.shutdown();
        return recommendations;
    }

    private Map<String, Object> convertSpotifyTrackToMap(JsonNode trackNode) {
        Map<String, Object> trackInfo = new HashMap<>();
        trackInfo.put("name", trackNode.get("name").asText());
        trackInfo.put("artist", trackNode.get("artists").get(0).get("name").asText());
        trackInfo.put("uri", trackNode.get("uri").asText());
        trackInfo.put("album", trackNode.get("album").get("name").asText());
        trackInfo.put("image", trackNode.get("album").get("images").get(0).get("url").asText());
        return trackInfo;
    }




    public List<Map<String, Object>> searchTracksByArtist(String artistName) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            String query = URLEncoder.encode(URLEncoder.encode("artist:" + artistName, StandardCharsets.UTF_8), StandardCharsets.UTF_8);
            String url = UriComponentsBuilder.fromHttpUrl(SPOTIFY_API_BASE_URL)
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", 30)
                    .toUriString();
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, Object>> tracks = (List<Map<String, Object>>) ((Map<String, Object>) response.getBody().get("tracks")).get("items");

            return tracks.stream().map(track -> Map.of(
                    "name", track.get("name"),
                    "artist", ((List<Map<String, Object>>) track.get("artists")).stream()
                            .map(artist -> artist.get("name").toString())
                            .collect(Collectors.joining(", ")),
                    "album", ((Map<String, Object>) track.get("album")).get("name"),
                    "images", ((Map<String, Object>) track.get("album")).get("images"),
                    "uri", track.get("uri")
            )).collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Error in searchTracksByArtist: " + e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> searchArtists(String query) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            String url = UriComponentsBuilder.fromHttpUrl(SPOTIFY_API_BASE_URL)
                    .queryParam("q", query)
                    .queryParam("type", "artist")
                    .queryParam("limit", 20)
                    .toUriString();
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            Map<String, Object> artists = (Map<String, Object>) body.get("artists");
            List<Map<String, Object>> artistItems = (List<Map<String, Object>>) artists.get("items");
            return artistItems.stream()
                    .map(artist -> Map.of(
                            "name", artist.get("name"),
                            "images", artist.get("images"),
                            "popularity", artist.get("popularity")
                    ))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in search: " + e.getMessage());
            throw new RuntimeException("Spotify API request failed", e);
        }
    }

    public List<Map<String, Object>> searchTracks(String query) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            String url = UriComponentsBuilder.fromHttpUrl(SPOTIFY_API_BASE_URL)
                    .queryParam("q", query)
                    .queryParam("type", "track")
                    .queryParam("limit", 20)
                    .toUriString();
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            Map<String, Object> tracks = (Map<String, Object>) body.get("tracks");
            List<Map<String, Object>> trackItems = (List<Map<String, Object>>) tracks.get("items");

            return trackItems.stream()
                    .map(track -> {
                        List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                        String artistNames = artists.stream()
                                .map(artist -> (String) artist.get("name"))
                                .collect(Collectors.joining(", "));

                        return Map.of(
                                "name", track.get("name"),
                                "artist", artistNames,
                                "album", ((Map<String, Object>) track.get("album")).get("name"),
                                "images", ((Map<String, Object>) track.get("album")).get("images"),
                                "uri", track.get("uri")
                        );
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in search: " + e.getMessage());
            throw new RuntimeException("Spotify API request failed", e);
        }
    }

    public List<Map<String, Object>> getTop10Artists(String playlistId) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            String url = SPOTIFY_PLAYLIST_URL + playlistId + "/tracks";
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode items = mapper.readTree(response.getBody()).get("items");
            if (items == null || items.isEmpty()) {
                throw new RuntimeException("No items found in the response");
            }
            Map<String, Integer> artistFrequency = new HashMap<>();
            Map<String, String> artistImages = new HashMap<>();
            for (JsonNode item : items) {
                JsonNode track = item.get("track");
                if (track != null) {
                    JsonNode album = track.get("album");
                    String imageUrl = null;
                    if (album != null && album.has("images")) {
                        JsonNode images = album.get("images");
                        if (images.size() > 0) {
                            imageUrl = images.get(0).get("url").asText();
                        }
                    }
                    JsonNode artists = track.get("artists");
                    if (artists != null) {
                        for (JsonNode artist : artists) {
                            String artistName = artist.get("name").asText();
                            artistFrequency.put(artistName, artistFrequency.getOrDefault(artistName, 0) + 1);
                            artistImages.putIfAbsent(artistName, imageUrl);
                        }
                    }
                }
            }

            return artistFrequency.entrySet().stream()
                    .sorted((a, b) -> b.getValue() - a.getValue())
                    .limit(10)
                    .map(entry -> {
                        Map<String, Object> artistMap = new HashMap<>();
                        artistMap.put("name", entry.getKey());
                        artistMap.put("image", artistImages.get(entry.getKey()));
                        return artistMap;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error in getTop10Artists: " + e.getMessage());
            throw new RuntimeException("Spotify API request failed", e);
        }
    }

    public List<Map<String, Object>> getTop10Songs(String playlistId) {
        try {
            String accessToken = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + accessToken);
            String url = SPOTIFY_PLAYLIST_URL + playlistId + "/tracks?limit=10";
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode items = mapper.readTree(response.getBody()).get("items");
            List<Map<String, Object>> songs = new ArrayList<>();

            for (JsonNode item : items) {
                JsonNode track = item.get("track");
                Map<String, Object> songData = new HashMap<>();
                songData.put("name", track.get("name").asText());
                songData.put("artist", track.get("artists").findValues("name").stream()
                        .map(JsonNode::asText)
                        .collect(Collectors.joining(", ")));
                songData.put("album", track.get("album").get("name").asText());
                songData.put("images", track.get("album").get("images"));
                songData.put("uri", track.get("uri").asText());

                songs.add(songData);
            }
            return songs;
        } catch (Exception e) {
            System.err.println("Error in getTop10Songs: " + e.getMessage());
            throw new RuntimeException("Spotify API request failed", e);
        }
    }

}

//    public void storeRefreshTokenForUser(String userId, String refreshToken) {
//        Users user = repo.findById(Integer.parseInt(userId))
//                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//        user.setSpotify(refreshToken);
//        repo.save(user);
//    }
//
//    public URI getSpotifyLoginUrl(String userId) {
//        String spotifyAuthUrl = String.format(
//                "https://accounts.spotify.com/authorize?client_id=%s&response_type=code&redirect_uri=%s&scope=%s&state=%s",
//                clientId,
//                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
//                URLEncoder.encode(scopes, StandardCharsets.UTF_8),
//                URLEncoder.encode(userId, StandardCharsets.UTF_8)
//        );
//        return URI.create(spotifyAuthUrl);
//    }
//
//    public String refreshAccessToken(String userId) {
//        Users user = repo.findById(Integer.parseInt(userId))
//                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
//        String refreshToken = user.getSpotify();
//        String tokenUrl = SPOTIFY_TOKEN_URL;
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.setBasicAuth(clientId, clientSecret);
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("grant_type", "refresh_token");
//        body.add("refresh_token", refreshToken);
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
//        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
//        Map<String, Object> responseBody = response.getBody();
//        if (responseBody != null && responseBody.containsKey("access_token")) {
//            return (String) responseBody.get("access_token");
//        } else {
//            throw new RuntimeException("Error refreshing access token from Spotify");
//        }
//    }
//
//    public List<Map<String, Object>> getUserPlaylists(String userId) {
//        try {
//            String accessToken = refreshAccessToken(userId);
//            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization", "Bearer " + accessToken);
//            String url = "https://api.spotify.com/v1/me/playlists";
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode playlists = mapper.readTree(response.getBody()).get("items");
//            List<Map<String, Object>> playlistDetails = new ArrayList<>();
//            for (JsonNode playlist : playlists) {
//                Map<String, Object> playlistData = new HashMap<>();
//                playlistData.put("id", playlist.get("id").asText());
//                playlistData.put("name", playlist.get("name").asText());
//                playlistData.put("description", playlist.get("description").asText());
//                playlistData.put("image", playlist.get("images").size() > 0
//                        ? playlist.get("images").get(0).get("url").asText()
//                        : null);
//                playlistData.put("tracks", playlist.get("tracks").get("total").asInt());
//                playlistDetails.add(playlistData);
//            }
//            return playlistDetails;
//        } catch (Exception e) {
//            System.err.println("Error in getUserPlaylists: " + e.getMessage());
//            throw new RuntimeException("Spotify API request failed", e);
//        }
//    }
//
//    public String exchangeAuthorizationCodeForTokens(String code, String userId) {
//        String tokenUrl = SPOTIFY_TOKEN_URL;
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//        headers.setBasicAuth(clientId, clientSecret);
//        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//        body.add("grant_type", "authorization_code");
//        body.add("code", code);
//        body.add("redirect_uri", redirectUri);
//        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
//        ResponseEntity<Map> response = restTemplate.exchange(tokenUrl, HttpMethod.POST, request, Map.class);
//        Map<String, Object> responseBody = response.getBody();
//        if (responseBody != null && responseBody.containsKey("refresh_token")) {
//            String refreshToken = (String) responseBody.get("refresh_token");
//            storeRefreshTokenForUser(userId, refreshToken);
//            return refreshToken;
//        } else {
//            throw new RuntimeException("Error fetching refresh token from Spotify");
//        }
//    }