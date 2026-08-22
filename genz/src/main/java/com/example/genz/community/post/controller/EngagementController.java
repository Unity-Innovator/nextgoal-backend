package com.example.genz.community.post.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.dto.EngagementLikeResponse;
import com.example.genz.community.post.service.EngagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class EngagementController {

    private final EngagementService engagementService;

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<EngagementLikeResponse>> likePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        EngagementLikeResponse response = engagementService.likePost(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post liked", response));
    }

    @DeleteMapping("/posts/{postId}/like")
    public ResponseEntity<ApiResponse<EngagementLikeResponse>> unlikePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        EngagementLikeResponse response = engagementService.unlikePost(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post unliked", response));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse response = engagementService.addComment(
                postId,
                request,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", response));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<ApiResponse<CommentResponse>> replyToComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse response = engagementService.replyToComment(
                commentId,
                request,
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Reply added", response));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> editComment(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request,
            Authentication authentication
    ) {
        CommentResponse response = engagementService.editComment(
                commentId,
                request,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Comment updated", response));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        engagementService.deleteComment(commentId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Comment deleted", null));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<EngagementLikeResponse>> likeComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        EngagementLikeResponse response = engagementService.likeComment(
                commentId,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Comment liked", response));
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<EngagementLikeResponse>> unlikeComment(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        EngagementLikeResponse response = engagementService.unlikeComment(
                commentId,
                authentication.getName()
        );
        return ResponseEntity.ok(ApiResponse.success("Comment unliked", response));
    }
}
