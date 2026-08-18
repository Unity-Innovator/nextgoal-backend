package com.example.genz.controller;

import com.example.genz.dto.NotificationDTO.NotificationResponse;
import com.example.genz.service.NotificationService.NotificationService;
import com.example.genz.exception.ApiResponse;
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

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(Authentication authentication) {
        String phoneNumber = authentication.getName();
        List<NotificationResponse> notifications = notificationService.getUserNotifications(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }
}
