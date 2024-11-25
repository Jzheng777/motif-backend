package com.jzk.motif_backend.repo;

import com.jzk.motif_backend.model.Songs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepo extends JpaRepository<Songs, Integer> {
    List<Songs> findByUserId(int userId);
}
