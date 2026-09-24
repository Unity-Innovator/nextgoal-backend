package com.example.genz.community.notification.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.notification.dto.CommunityNotificationResponse;
import com.example.genz.community.notification.dto.MarkAllNotificationsReadResponse;
import com.example.genz.community.notification.dto.NotificationCursorPageResponse;
import com.example.genz.community.notification.dto.NotificationCursorResponse;
import com.example.genz.community.notification.dto.UnreadNotificationCountResponse;
import com.example.genz.community.notification.entity.Notification;
import com.example.genz.community.notification.enums.NotificationReferenceType;
import com.example.genz.community.notification.enums.NotificationType;
import com.example.genz.community.notification.event.CommunityNotificationCreatedEvent;
import com.example.genz.community.notification.exception.NotificationBadRequestException;
import com.example.genz.community.notification.exception.NotificationNotFoundException;
import com.example.genz.community.notification.mapper.CommunityNotificationMapper;
import com.example.genz.community.notification.repository.CommunityNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityNotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final CommunityNotificationRepository notificationRepository;
    private final CommunityNotificationMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NotificationCursorPageResponse getNotifications(
            String currentPhoneNumber,
            boolean unreadOnly,
            Instant cursorCreatedAt,
            UUID cursorNotificationId,
            int limit
    ) {
        User recipient = requireCurrentUser(currentPhoneNumber);
        validateCursor(cursorCreatedAt, cursorNotificationId);
        List<Notification> notifications = notificationRepository.findNotifications(
                recipient.getId(),
                unreadOnly,
                cursorCreatedAt,
                cursorNotificationId,
                cursorPageRequest(limit)
        );

        boolean hasNext = notifications.size() > limit;
        List<Notification> pageNotifications = hasNext
                ? notifications.subList(0, limit)
                : notifications;
        List<CommunityNotificationResponse> items = pageNotifications.stream()
                .map(mapper::toResponse)
                .toList();

        NotificationCursorResponse nextCursor = null;
        if (hasNext && !pageNotifications.isEmpty()) {
            Notification last = pageNotifications.getLast();
            nextCursor = new NotificationCursorResponse(last.getCreatedAt(), last.getId());
        }
        return new NotificationCursorPageResponse(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(String currentPhoneNumber) {
        User recipient = requireCurrentUser(currentPhoneNumber);
        return new UnreadNotificationCountResponse(
                notificationRepository.countByRecipient_IdAndReadAtIsNull(recipient.getId())
        );
    }

    @Transactional
    public CommunityNotificationResponse markAsRead(
            UUID notificationId,
            String currentPhoneNumber
    ) {
        User recipient = requireCurrentUser(currentPhoneNumber);
        Notification notification = notificationRepository.findOwnedForUpdate(
                        notificationId,
                        recipient.getId()
                )
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notificationRepository.saveAndFlush(notification);
        }
        return mapper.toResponse(notification);
    }

    @Transactional
    public MarkAllNotificationsReadResponse markAllAsRead(String currentPhoneNumber) {
        User recipient = requireCurrentUser(currentPhoneNumber);
        int markedReadCount = notificationRepository.markAllRead(recipient.getId(), Instant.now());
        return new MarkAllNotificationsReadResponse(markedReadCount, 0);
    }

    public void notifyFriendRequest(User recipient, User actor, UUID requestId) {
        create(
                recipient,
                actor,
                NotificationType.FRIEND_REQUEST,
                NotificationReferenceType.FRIEND_REQUEST,
                requestId,
                payload(actor, Map.of("requestId", requestId.toString()))
        );
    }

    public void notifyFriendAccepted(User recipient, User actor, UUID requestId) {
        create(
                recipient,
                actor,
                NotificationType.FRIEND_ACCEPTED,
                NotificationReferenceType.FRIEND_REQUEST,
                requestId,
                payload(actor, Map.of("requestId", requestId.toString()))
        );
    }

    public void notifyPostLiked(User recipient, User actor, UUID postId) {
        create(
                recipient,
                actor,
                NotificationType.POST_LIKED,
                NotificationReferenceType.POST,
                postId,
                payload(actor, Map.of("postId", postId.toString()))
        );
    }

    public void notifyPostCommented(User recipient, User actor, UUID postId, UUID commentId) {
        create(
                recipient,
                actor,
                NotificationType.POST_COMMENTED,
                NotificationReferenceType.COMMENT,
                commentId,
                payload(actor, Map.of(
                        "postId", postId.toString(),
                        "commentId", commentId.toString()
                ))
        );
    }

    public void notifyCommentReplied(
            User recipient,
            User actor,
            UUID postId,
            UUID parentCommentId,
            UUID replyId
    ) {
        create(
                recipient,
                actor,
                NotificationType.COMMENT_REPLIED,
                NotificationReferenceType.COMMENT,
                replyId,
                payload(actor, Map.of(
                        "postId", postId.toString(),
                        "parentCommentId", parentCommentId.toString(),
                        "replyId", replyId.toString()
                ))
        );
    }

    public void notifyPostShared(User recipient, User actor, UUID sourcePostId, UUID sharePostId) {
        create(
                recipient,
                actor,
                NotificationType.POST_SHARED,
                NotificationReferenceType.POST,
                sourcePostId,
                payload(actor, Map.of(
                        "sourcePostId", sourcePostId.toString(),
                        "sharePostId", sharePostId.toString()
                ))
        );
    }

    private void create(
            User recipient,
            User actor,
            NotificationType type,
            NotificationReferenceType referenceType,
            UUID referenceId,
            Map<String, Object> payload
    ) {
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setPayload(payload);
        notificationRepository.saveAndFlush(notification);

        CommunityNotificationResponse response = mapper.toResponse(notification);
        eventPublisher.publishEvent(new CommunityNotificationCreatedEvent(
                recipient.getPhoneNumber(),
                response
        ));
    }

    private Map<String, Object> payload(User actor, Map<String, Object> values) {
        Map<String, Object> payload = new HashMap<>(values);
        payload.put("actorFullName", actor.getFullName());
        return payload;
    }

    private User requireCurrentUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotificationNotFoundException("Authenticated user was not found"));
    }

    private void validateCursor(Instant cursorCreatedAt, UUID cursorNotificationId) {
        if ((cursorCreatedAt == null) != (cursorNotificationId == null)) {
            throw new NotificationBadRequestException(
                    "cursorCreatedAt and cursorNotificationId must be provided together"
            );
        }
    }

    private PageRequest cursorPageRequest(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new NotificationBadRequestException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(0, limit + 1);
    }
}
