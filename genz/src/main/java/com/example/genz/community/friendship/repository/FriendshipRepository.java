package com.example.genz.community.friendship.repository;

import com.example.genz.community.friendship.entity.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FriendshipRepository extends JpaRepository<Friendship, UUID> {

    boolean existsByUserOne_IdAndUserTwo_Id(UUID userOneId, UUID userTwoId);

    @EntityGraph(attributePaths = {"userOne", "userTwo"})
    Optional<Friendship> findByUserOne_IdAndUserTwo_Id(UUID userOneId, UUID userTwoId);

    @EntityGraph(attributePaths = {"userOne", "userTwo"})
    Page<Friendship> findByUserOne_IdOrUserTwo_IdOrderByCreatedAtDesc(
            UUID userOneId,
            UUID userTwoId,
            Pageable pageable
    );
}
