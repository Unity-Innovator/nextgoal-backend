package com.example.genz.community.notification.repository;

import com.example.genz.community.notification.entity.Notification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommunityNotificationRepository extends JpaRepository<Notification, UUID> {

    // PostgreSQL needs an explicit type for a null cursor in the IS NULL guard.
    @Query("""
            select n
            from CommunityNotification n
            left join fetch n.actor
            where n.recipient.id = :recipientId
              and (:unreadOnly = false or n.readAt is null)
              and (
                  cast(:cursorCreatedAt as timestamp) is null
                  or n.createdAt < :cursorCreatedAt
                  or (n.createdAt = :cursorCreatedAt and n.id < :cursorNotificationId)
              )
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findNotifications(
            @Param("recipientId") UUID recipientId,
            @Param("unreadOnly") boolean unreadOnly,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorNotificationId") UUID cursorNotificationId,
            Pageable pageable
    );

    long countByRecipient_IdAndReadAtIsNull(UUID recipientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select n
            from CommunityNotification n
            left join fetch n.actor
            where n.id = :notificationId and n.recipient.id = :recipientId
            """)
    Optional<Notification> findOwnedForUpdate(
            @Param("notificationId") UUID notificationId,
            @Param("recipientId") UUID recipientId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CommunityNotification n
            set n.readAt = :readAt
            where n.recipient.id = :recipientId and n.readAt is null
            """)
    int markAllRead(
            @Param("recipientId") UUID recipientId,
            @Param("readAt") Instant readAt
    );
}
