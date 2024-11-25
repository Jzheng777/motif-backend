package com.jzk.motif_backend.repo;

import com.jzk.motif_backend.model.Friends;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FriendRepo extends JpaRepository<Friends, Integer> {
    List<Friends>findByUserId(int userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Friends f WHERE (f.userId = :userId AND f.friendId = :friendId) OR (f.userId = :friendId AND f.friendId = :userId)")
    void deleteMutualFriendship(int userId, int friendId);

    @Query("SELECT f FROM Friends f WHERE f.userId = :userId OR f.friendId = :userId")
    List<Friends> findFriendsByUserId(@Param("userId") int userId);

}
