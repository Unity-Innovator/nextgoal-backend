package com.example.genz.community.friendship.repository;

import com.example.genz.community.friendship.entity.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlocker_IdAndBlockedUser_Id(UUID blockerId, UUID blockedUserId);

    @EntityGraph(attributePaths = {"blocker", "blockedUser"})
    Optional<UserBlock> findByBlocker_IdAndBlockedUser_Id(UUID blockerId, UUID blockedUserId);

    @EntityGraph(attributePaths = "blockedUser")
    Page<UserBlock> findByBlocker_IdOrderByCreatedAtDesc(UUID blockerId, Pageable pageable);
}
