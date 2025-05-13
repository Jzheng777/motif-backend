package com.jzk.motif_backend.controller;

import com.jzk.motif_backend.dto.UserDTO;
import com.jzk.motif_backend.model.Users;
import com.jzk.motif_backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

//    @PostMapping("/auth/register")
//    public Users register(@RequestBody Users user) {
//        return service.register(user);
//    }

    @PostMapping("/auth/register")
    public Map<String, Object> register(@RequestBody Users user) {
        return service.registerAndAuthenticate(user);
    }


    @GetMapping("/api/user/{id}")
    public Map<String, Object> profile(@PathVariable Integer id) {
        return service.getByUserId(id);
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Users user) {
        return service.verify(user);
    }

    @PutMapping("/api/user/{id}")
    public Map<String, Object> updateProfile(@PathVariable Integer id,
                                             @RequestParam(required = false) String username,
                                             @RequestParam(required = true) String password,
                                             @RequestParam(required = false) MultipartFile profile_picture) {
        return service.updateProfile(id, username, password, profile_picture);
    }

    @GetMapping("/api/users")
    public List<UserDTO> getAllUsers() {
        return service.getAllUsers();
    }
}
