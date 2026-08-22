package com.example.genz.community.moderation.dto;

import com.example.genz.community.post.enums.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewReportRequest(
        @NotNull ReportStatus status,
        @Size(max = 1000) String moderationNote
) {
}
