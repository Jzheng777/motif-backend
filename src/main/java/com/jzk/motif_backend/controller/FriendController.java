package com.jzk.motif_backend.controller;


import com.jzk.motif_backend.model.Friends;
import com.jzk.motif_backend.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class FriendController {
    public final FriendService service;
    public FriendController(FriendService service) { this.service = service; }

    @PostMapping("/friend/add")
    public ResponseEntity<String> addFriend(@RequestBody List<Friends> friendsList) {
        if (friendsList.size() == 2) {
            String message = service.saveFriends(friendsList);
            return ResponseEntity.ok(message);
        } else {
            return ResponseEntity.badRequest().body("Please provide exactly two Friends objects to establish mutual friendship.");
        }
    }

    @GetMapping("/friend/{userId}")
    public ResponseEntity<List<Friends>> getFriends(@PathVariable int userId) {
        List<Friends> friends = service.getFriendsByUserId(userId);
        return ResponseEntity.ok(friends);
    }

    @DeleteMapping("/friend/{userId}/{friendId}")
    public ResponseEntity<String> deleteFriend(@PathVariable int userId, @PathVariable int friendId) {
        String message = service.deleteFriends(userId, friendId);
        return ResponseEntity.ok(message);
    }

}
