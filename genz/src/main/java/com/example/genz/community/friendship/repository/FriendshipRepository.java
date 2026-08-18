package com.example.genz.community.friendship.repository;

import com.example.genz.community.friendship.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    boolean existsByUserOne_IdAndUserTwo_Id(UUID userOneId, UUID userTwoId);
}
