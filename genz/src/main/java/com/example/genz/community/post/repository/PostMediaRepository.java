package com.example.genz.community.post.repository;

import com.example.genz.community.post.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {

    @Modifying(flushAutomatically = true)
    @Query("delete from PostMedia media where media.post.id = :postId")
    void deleteAllByPostId(@Param("postId") UUID postId);
}
