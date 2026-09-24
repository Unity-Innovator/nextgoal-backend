package com.example.genz.community.notification.mapper;

import com.example.genz.auth.entity.User;
import com.example.genz.community.notification.dto.CommunityNotificationResponse;
import com.example.genz.community.notification.dto.NotificationActorResponse;
import com.example.genz.community.notification.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommunityNotificationMapper {

    public CommunityNotificationResponse toResponse(Notification notification) {
        User actor = notification.getActor();
        NotificationActorResponse actorResponse = actor == null
                ? null
                : new NotificationActorResponse(actor.getId(), actor.getFullName());
        Map<String, Object> payload = notification.getPayload() == null
                ? Map.of()
                : Map.copyOf(notification.getPayload());
        return new CommunityNotificationResponse(
                notification.getId(),
                actorResponse,
                notification.getType(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                payload,
                notification.getReadAt() != null,
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
