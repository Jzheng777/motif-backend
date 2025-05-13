package com.jzk.motif_backend.service;

import com.jzk.motif_backend.dto.UserDTO;
import com.jzk.motif_backend.model.Users;
import com.jzk.motif_backend.repo.UserRepo;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepo repo;
    private final JWTService jwtService;
    private final AuthenticationManager authenticationManager;
    private final BCryptPasswordEncoder encoder;

    public UserService(UserRepo repo, JWTService jwtService, AuthenticationManager authenticationManager) {
        this.repo = repo;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.encoder = new BCryptPasswordEncoder(12);
    }

    public Users register(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);
        return user;
    }

    public Map<String, Object> registerAndAuthenticate(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        repo.save(user);

        // Authenticate and return token like in verify()
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );

        if (auth.isAuthenticated()) {
            Users authenticatedUser = repo.findByUsername(user.getUsername());
            String token = jwtService.generateToken(authenticatedUser.getUsername());
            int userId = authenticatedUser.getId();

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("userId", userId);
            return response;
        } else {
            throw new RuntimeException("Authentication failed after registration");
        }
    }


    public Map<String, Object> verify(Users user) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
            );
            if (auth.isAuthenticated()) {
                Users authenticatedUser = repo.findByUsername(user.getUsername());
                if (authenticatedUser != null) {
                    String token = jwtService.generateToken(authenticatedUser.getUsername());
                    int userId = authenticatedUser.getId();  // Now the ID should be populated
                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("userId", userId);
                    return response;
                } else {
                    throw new RuntimeException("User not found");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid credentials");
        }
        return Collections.emptyMap();
    }

    public Map<String, Object> getByUserId(int userId) {
        Users user = repo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("username", user.getUsername());
        String profilePicUrl = null;
        if (user.getProfile_pic() != null) {
            profilePicUrl = "http://localhost:8080" + user.getProfile_pic();
        }
        profileData.put("pic", profilePicUrl);
        return Map.of("profile", profileData);
    }

    public Map<String, Object> updateProfile(int userId, String newUsername, String password, MultipartFile profilePicture) {
        Users user = repo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        if (!encoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Incorrect password");
        }
        if (newUsername != null && !newUsername.isEmpty()) {
            user.setUsername(newUsername);
        }
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                String fileName = "user_" + userId + "_" + profilePicture.getOriginalFilename();
                Path picturePath = Paths.get("src/main/resources/static/profile_pics").resolve(fileName);
                Files.createDirectories(picturePath.getParent());
                Files.copy(profilePicture.getInputStream(), picturePath, StandardCopyOption.REPLACE_EXISTING);
                user.setProfile_pic("/profile_pics/" + fileName);
            } catch (Exception e) {
                throw new RuntimeException("Error saving profile picture", e);
            }
        }
        repo.save(user);
        String newToken = jwtService.generateToken(user.getUsername());
        Map<String, Object> response = new HashMap<>();
        response.put("profile", Map.of("username", user.getUsername(), "pic", user.getProfile_pic()));
        response.put("token", newToken);

        return response;
    }

    public List<UserDTO> getAllUsers() {
        List<Users> users = repo.findAll();
        return users.stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername(), user.getProfile_pic(), user.getSpotify()))
                .collect(Collectors.toList());
    }



}
