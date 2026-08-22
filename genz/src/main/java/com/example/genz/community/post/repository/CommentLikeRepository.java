package com.example.genz.community.post.repository;

import com.example.genz.community.post.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {

    Optional<CommentLike> findByComment_IdAndUser_Id(UUID commentId, UUID userId);

    boolean existsByComment_IdAndUser_Id(UUID commentId, UUID userId);

    long countByComment_Id(UUID commentId);
}
