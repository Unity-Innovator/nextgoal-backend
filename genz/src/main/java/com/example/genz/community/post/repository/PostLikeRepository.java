package com.example.genz.community.post.repository;

import com.example.genz.community.post.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    Optional<PostLike> findByPost_IdAndUser_Id(UUID postId, UUID userId);

    long countByPost_Id(UUID postId);
}
