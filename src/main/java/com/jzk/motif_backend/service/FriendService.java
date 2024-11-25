package com.jzk.motif_backend.service;

import com.jzk.motif_backend.model.Friends;
import com.jzk.motif_backend.repo.FriendRepo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FriendService {
    private FriendRepo repo;
    public FriendService(FriendRepo repo) { this.repo = repo; }

    public List<Friends> getAllFriends() {
        return repo.findAll();
    }

    public List<Friends> getFriendsByUserId(int userId) {
        return repo.findFriendsByUserId(userId);
    }

    public String saveFriends(List<Friends> friendsList) {
        if (friendsList.size() == 2) {
            repo.saveAll(friendsList);
            return "Friend successfully added.";
        } else {
            throw new IllegalArgumentException("Two Friends objects are required to establish a mutual friendship.");
        }
    }

    public String deleteFriends(int userId, int friendId) {
        repo.deleteMutualFriendship(userId, friendId);
        return "Friendship successfully deleted.";
    }
}
