package com.example.genz.community.notification.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.notification.dto.CommunityNotificationResponse;
import com.example.genz.community.notification.dto.MarkAllNotificationsReadResponse;
import com.example.genz.community.notification.dto.NotificationCursorPageResponse;
import com.example.genz.community.notification.dto.UnreadNotificationCountResponse;
import com.example.genz.community.notification.service.CommunityNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/community/notifications")
@RequiredArgsConstructor
public class CommunityNotificationController {

    private final CommunityNotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationCursorPageResponse>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorNotificationId,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        NotificationCursorPageResponse response = notificationService.getNotifications(
                authentication.getName(),
                unreadOnly,
                cursorCreatedAt,
                cursorNotificationId,
                limit
        );
        return ResponseEntity.ok(ApiResponse.success("Community notifications retrieved", response));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCountResponse>> getUnreadCount(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Unread notification count retrieved",
                notificationService.getUnreadCount(authentication.getName())
        ));
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<CommunityNotificationResponse>> markAsRead(
            @PathVariable UUID notificationId,
            Authentication authentication
    ) {
        CommunityNotificationResponse response = notificationService.markAsRead(
                notificationId,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<MarkAllNotificationsReadResponse>> markAllAsRead(
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "All notifications marked as read",
                notificationService.markAllAsRead(authentication.getName())
        ));
    }
}
