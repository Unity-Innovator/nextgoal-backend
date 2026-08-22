package com.example.genz.community.friendship.repository;

import com.example.genz.community.friendship.entity.FriendRequest;
import com.example.genz.community.friendship.enums.FriendRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, UUID> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    Page<FriendRequest> findByReceiver_IdAndStatusOrderByCreatedAtDesc(
            UUID receiverId,
            FriendRequestStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"sender", "receiver"})
    Page<FriendRequest> findBySender_IdAndStatusOrderByCreatedAtDesc(
            UUID senderId,
            FriendRequestStatus status,
            Pageable pageable
    );

    boolean existsBySender_IdAndReceiver_IdAndStatus(
            UUID senderId,
            UUID receiverId,
            FriendRequestStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select request
            from FriendRequest request
            join fetch request.sender
            join fetch request.receiver
            where request.id = :requestId
            """)
    Optional<FriendRequest> findByIdForUpdate(@Param("requestId") UUID requestId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update FriendRequest request
            set request.status = com.example.genz.community.friendship.enums.FriendRequestStatus.CANCELLED,
                request.respondedAt = :respondedAt,
                request.updatedAt = :respondedAt
            where request.status = com.example.genz.community.friendship.enums.FriendRequestStatus.PENDING
              and ((request.sender.id = :firstUserId and request.receiver.id = :secondUserId)
                or (request.sender.id = :secondUserId and request.receiver.id = :firstUserId))
            """)
    int cancelPendingBetween(
            @Param("firstUserId") UUID firstUserId,
            @Param("secondUserId") UUID secondUserId,
            @Param("respondedAt") Instant respondedAt
    );
}
