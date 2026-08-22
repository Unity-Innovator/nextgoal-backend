package com.example.genz.community.moderation.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.moderation.dto.ModerationPageResponse;
import com.example.genz.community.moderation.dto.ModerationReportResponse;
import com.example.genz.community.moderation.dto.ReviewReportRequest;
import com.example.genz.community.moderation.service.CommunityModerationService;
import com.example.genz.community.post.enums.ReportStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/community/admin")
@RequiredArgsConstructor
public class AdminModerationController {

    private final CommunityModerationService moderationService;

    @GetMapping("/reports/posts")
    public ResponseEntity<ApiResponse<ModerationPageResponse<ModerationReportResponse>>> getPostReports(
            @RequestParam(required = false, defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post reports retrieved",
                moderationService.getPostReports(status, page, size, authentication.getName())
        ));
    }

    @GetMapping("/reports/comments")
    public ResponseEntity<ApiResponse<ModerationPageResponse<ModerationReportResponse>>> getCommentReports(
            @RequestParam(required = false, defaultValue = "PENDING") ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment reports retrieved",
                moderationService.getCommentReports(status, page, size, authentication.getName())
        ));
    }

    @PutMapping("/reports/posts/{reportId}")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> reviewPostReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Post report reviewed",
                moderationService.reviewPostReport(reportId, request, authentication.getName())
        ));
    }

    @PutMapping("/reports/comments/{reportId}")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> reviewCommentReport(
            @PathVariable UUID reportId,
            @Valid @RequestBody ReviewReportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "Comment report reviewed",
                moderationService.reviewCommentReport(reportId, request, authentication.getName())
        ));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> removePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        moderationService.removePost(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post hidden by moderator", null));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> removeComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        moderationService.removeComment(commentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Comment hidden by moderator", null));
    }
}
