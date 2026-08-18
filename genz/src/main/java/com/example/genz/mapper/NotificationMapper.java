package com.example.genz.mapper;

import com.example.genz.dto.NotificationDTO.NotificationResponse;
import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Notification.Notification;
import com.example.genz.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class NotificationMapper {

    public Notification toEntity(User user, String title, String message, NotificationType type, UUID referenceId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        return notification;
    }

    public NotificationResponse toNotificationResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType() != null ? notification.getType().name() : null)
                .referenceId(notification.getReferenceId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public List<NotificationResponse> toNotificationResponseList(List<Notification> notifications) {
        if (notifications == null) {
            return Collections.emptyList();
        }
        return notifications.stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }
}
