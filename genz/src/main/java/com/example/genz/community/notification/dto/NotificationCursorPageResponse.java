package com.example.genz.community.notification.dto;

import java.util.List;

public record NotificationCursorPageResponse(
        List<CommunityNotificationResponse> items,
        NotificationCursorResponse nextCursor,
        boolean hasNext
) {
}
