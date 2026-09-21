package com.example.genz.notification.controller;

import com.example.genz.notification.dto.NotificationResponse;
import com.example.genz.notification.service.NotificationService;
import com.example.genz.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final com.example.genz.notification.repository.NotificationRepository notifications;
    private final com.example.genz.auth.repository.UserRepository users;

    @org.springframework.web.bind.annotation.PostMapping("/{id}/read")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> read(@org.springframework.web.bind.annotation.PathVariable java.util.UUID id,
                                     Authentication authentication) {
        var user = users.findByPhoneNumber(authentication.getName()).orElseThrow();
        var item = notifications.findById(id)
                .filter(n -> n.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
        item.setRead(true);
        notifications.save(item);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.PostMapping("/read-all")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> readAll(Authentication authentication) {
        var user = users.findByPhoneNumber(authentication.getName()).orElseThrow();
        var items = notifications.findByUserIdOrderByCreatedAtDesc(user.getId());
        items.forEach(n -> n.setRead(true));
        notifications.saveAll(items);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(Authentication authentication) {
        String phoneNumber = authentication.getName();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }
}
