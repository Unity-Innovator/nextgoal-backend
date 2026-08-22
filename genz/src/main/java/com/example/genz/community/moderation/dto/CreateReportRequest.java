package com.example.genz.community.moderation.dto;

import com.example.genz.community.post.enums.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReportRequest(
        @NotNull ReportReason reason,
        @Size(max = 1000) String details
) {
}
