package com.example.genz.community.moderation.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.moderation.dto.CreateReportRequest;
import com.example.genz.community.moderation.dto.ModerationReportResponse;
import com.example.genz.community.moderation.service.CommunityModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityReportController {

    private final CommunityModerationService moderationService;

    @PostMapping("/posts/{postId}/reports")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> reportPost(
            @PathVariable UUID postId,
            @Valid @RequestBody CreateReportRequest request,
            Authentication authentication
    ) {
        ModerationReportResponse response = moderationService.reportPost(
                postId,
                request,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post reported", response));
    }

    @PostMapping("/comments/{commentId}/reports")
    public ResponseEntity<ApiResponse<ModerationReportResponse>> reportComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CreateReportRequest request,
            Authentication authentication
    ) {
        ModerationReportResponse response = moderationService.reportComment(
                commentId,
                request,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment reported", response));
    }
}
