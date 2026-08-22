package com.example.genz.community.post.repository;

import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.enums.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    @EntityGraph(attributePaths = {"author", "goal", "sharedPost", "media"})
    Optional<Post> findByIdAndStatus(UUID postId, PostStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p join fetch p.author where p.id = :postId and p.status = :status")
    Optional<Post> findByIdAndStatusForUpdate(
            @Param("postId") UUID postId,
            @Param("status") PostStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Post p where p.id = :postId")
    Optional<Post> findByIdForUpdate(@Param("postId") UUID postId);

    @Query("""
            select p
            from Post p
            where p.status = com.example.genz.community.post.enums.PostStatus.ACTIVE
              and p.visibility = com.example.genz.community.post.enums.PostVisibility.PUBLIC
              and (
                  p.author.id = :viewerId
                  or not exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                  )
                  or exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                        and settings.profileVisibility = com.example.genz.community.post.enums.PostVisibility.PUBLIC
                  )
                  or (
                      exists (
                          select settings.id from UserSettings settings
                          where settings.user.id = p.author.id
                            and settings.profileVisibility = com.example.genz.community.post.enums.PostVisibility.FRIENDS_ONLY
                      )
                      and exists (
                          select profileFriendship.id from Friendship profileFriendship
                          where (profileFriendship.userOne.id = :viewerId and profileFriendship.userTwo.id = p.author.id)
                             or (profileFriendship.userOne.id = p.author.id and profileFriendship.userTwo.id = :viewerId)
                      )
                  )
              )
              and not exists (
                  select b.id
                  from UserBlock b
                  where (b.blocker.id = :viewerId and b.blockedUser.id = p.author.id)
                     or (b.blocker.id = p.author.id and b.blockedUser.id = :viewerId)
              )
              and (
                  :cursorCreatedAt is null
                  or p.createdAt < :cursorCreatedAt
                  or (p.createdAt = :cursorCreatedAt and p.id < :cursorPostId)
              )
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findPublicFeed(
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorPostId") UUID cursorPostId,
            Pageable pageable
    );

    @Query("""
            select p
            from Post p
            where p.status = com.example.genz.community.post.enums.PostStatus.ACTIVE
              and p.visibility in (
                  com.example.genz.community.post.enums.PostVisibility.PUBLIC,
                  com.example.genz.community.post.enums.PostVisibility.FRIENDS_ONLY
              )
              and (
                  not exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                  )
                  or exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                        and settings.profileVisibility in (
                            com.example.genz.community.post.enums.PostVisibility.PUBLIC,
                            com.example.genz.community.post.enums.PostVisibility.FRIENDS_ONLY
                        )
                  )
              )
              and exists (
                  select f.id
                  from Friendship f
                  where (f.userOne.id = :viewerId and f.userTwo.id = p.author.id)
                     or (f.userOne.id = p.author.id and f.userTwo.id = :viewerId)
              )
              and not exists (
                  select b.id
                  from UserBlock b
                  where (b.blocker.id = :viewerId and b.blockedUser.id = p.author.id)
                     or (b.blocker.id = p.author.id and b.blockedUser.id = :viewerId)
              )
              and (
                  :cursorCreatedAt is null
                  or p.createdAt < :cursorCreatedAt
                  or (p.createdAt = :cursorCreatedAt and p.id < :cursorPostId)
              )
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findFriendsFeed(
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorPostId") UUID cursorPostId,
            Pageable pageable
    );

    @Query("""
            select p
            from Post p
            where p.author.id = :authorId
              and p.status = com.example.genz.community.post.enums.PostStatus.ACTIVE
              and not exists (
                  select b.id
                  from UserBlock b
                  where (b.blocker.id = :viewerId and b.blockedUser.id = p.author.id)
                     or (b.blocker.id = p.author.id and b.blockedUser.id = :viewerId)
              )
              and (
                  p.author.id = :viewerId
                  or not exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                  )
                  or exists (
                      select settings.id from UserSettings settings
                      where settings.user.id = p.author.id
                        and settings.profileVisibility = com.example.genz.community.post.enums.PostVisibility.PUBLIC
                  )
                  or (
                      exists (
                          select settings.id from UserSettings settings
                          where settings.user.id = p.author.id
                            and settings.profileVisibility = com.example.genz.community.post.enums.PostVisibility.FRIENDS_ONLY
                      )
                      and exists (
                          select profileFriendship.id from Friendship profileFriendship
                          where (profileFriendship.userOne.id = :viewerId and profileFriendship.userTwo.id = p.author.id)
                             or (profileFriendship.userOne.id = p.author.id and profileFriendship.userTwo.id = :viewerId)
                      )
                  )
              )
              and (
                  p.author.id = :viewerId
                  or p.visibility = com.example.genz.community.post.enums.PostVisibility.PUBLIC
                  or (
                      p.visibility = com.example.genz.community.post.enums.PostVisibility.FRIENDS_ONLY
                      and exists (
                          select f.id
                          from Friendship f
                          where (f.userOne.id = :viewerId and f.userTwo.id = p.author.id)
                             or (f.userOne.id = p.author.id and f.userTwo.id = :viewerId)
                      )
                  )
              )
              and (
                  :cursorCreatedAt is null
                  or p.createdAt < :cursorCreatedAt
                  or (p.createdAt = :cursorCreatedAt and p.id < :cursorPostId)
              )
            order by p.createdAt desc, p.id desc
            """)
    List<Post> findProfileFeed(
            @Param("authorId") UUID authorId,
            @Param("viewerId") UUID viewerId,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorPostId") UUID cursorPostId,
            Pageable pageable
    );

}
