package com.example.genz.community.post.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.PostCursorPageResponse;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.dto.SharePostRequest;
import com.example.genz.community.post.dto.UpdatePostRequest;
import com.example.genz.community.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            Authentication authentication
    ) {
        PostResponse response = postService.createPost(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post created", response));
    }

    @PutMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> updatePost(
            @PathVariable UUID postId,
            @Valid @RequestBody UpdatePostRequest request,
            Authentication authentication
    ) {
        PostResponse response = postService.updatePost(postId, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post updated", response));
    }

    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        postService.deletePost(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostResponse>> getPost(
            @PathVariable UUID postId,
            Authentication authentication
    ) {
        PostResponse response = postService.getPost(postId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Post retrieved", response));
    }

    @PostMapping("/posts/{postId}/share")
    public ResponseEntity<ApiResponse<PostResponse>> sharePost(
            @PathVariable UUID postId,
            @Valid @RequestBody SharePostRequest request,
            Authentication authentication
    ) {
        PostResponse response = postService.sharePost(postId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post shared", response));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<ApiResponse<PostCursorPageResponse>> getUserPosts(
            @PathVariable UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorPostId,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        PostCursorPageResponse response = postService.getUserPosts(
                userId,
                authentication.getName(),
                cursorCreatedAt,
                cursorPostId,
                limit
        );
        return ResponseEntity.ok(ApiResponse.success("User posts retrieved", response));
    }

    @GetMapping({"/feed", "/feed/public"})
    public ResponseEntity<ApiResponse<PostCursorPageResponse>> getPublicFeed(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorPostId,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        PostCursorPageResponse response = postService.getPublicFeed(
                authentication.getName(),
                cursorCreatedAt,
                cursorPostId,
                limit
        );
        return ResponseEntity.ok(ApiResponse.success("Public feed retrieved", response));
    }

    @GetMapping("/feed/friends")
    public ResponseEntity<ApiResponse<PostCursorPageResponse>> getFriendsFeed(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant cursorCreatedAt,
            @RequestParam(required = false) UUID cursorPostId,
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication
    ) {
        PostCursorPageResponse response = postService.getFriendsFeed(
                authentication.getName(),
                cursorCreatedAt,
                cursorPostId,
                limit
        );
        return ResponseEntity.ok(ApiResponse.success("Friends feed retrieved", response));
    }
}
