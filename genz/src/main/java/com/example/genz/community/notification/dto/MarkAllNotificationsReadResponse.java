package com.example.genz.community.notification.dto;

public record MarkAllNotificationsReadResponse(
        int markedReadCount,
        long unreadCount
) {
}
