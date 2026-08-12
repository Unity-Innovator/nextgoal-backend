package com.example.genz.Repository.NotificationRepository;

import com.example.genz.Entity.Notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Notification> findByReferenceIdAndUserId(UUID referenceId, UUID userId);
}
