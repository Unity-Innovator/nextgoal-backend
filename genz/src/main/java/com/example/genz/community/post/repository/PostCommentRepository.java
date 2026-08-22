package com.example.genz.community.post.repository;

import com.example.genz.community.post.entity.PostComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PostCommentRepository extends JpaRepository<PostComment, UUID> {

    @Query("""
            select c
            from PostComment c
            join fetch c.post p
            join fetch p.author
            join fetch c.author
            left join fetch c.parentComment
            where c.id = :commentId
            """)
    Optional<PostComment> findDetailedById(@Param("commentId") UUID commentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from PostComment c where c.id = :commentId")
    Optional<PostComment> findByIdForUpdate(@Param("commentId") UUID commentId);

    long countByPost_IdAndStatus(UUID postId, com.example.genz.community.post.enums.CommentStatus status);

    long countByParentComment_IdAndStatus(
            UUID parentCommentId,
            com.example.genz.community.post.enums.CommentStatus status
    );
}
