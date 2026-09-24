package com.example.genz.community.moderation.dto;

import com.example.genz.community.moderation.enums.ReportTargetType;
import com.example.genz.community.post.enums.ReportReason;
import com.example.genz.community.post.enums.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ModerationReportResponse(
        UUID id,
        ReportTargetType targetType,
        UUID targetId,
        UUID targetAuthorId,
        String targetContent,
        ModerationUserResponse reportedBy,
        ReportReason reason,
        String details,
        ReportStatus status,
        ModerationUserResponse reviewedBy,
        String moderationNote,
        Instant reviewedAt,
        Instant createdAt
) {
}
