package com.example.genz.community.friendship.repository;

import com.example.genz.community.friendship.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlocker_IdAndBlockedUser_Id(UUID blockerId, UUID blockedUserId);
}
