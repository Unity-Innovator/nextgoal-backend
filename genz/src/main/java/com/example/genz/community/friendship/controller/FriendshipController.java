package com.example.genz.community.friendship.controller;

import com.example.genz.common.api.ApiResponse;
import com.example.genz.community.friendship.dto.BlockedUserResponse;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.dto.FriendResponse;
import com.example.genz.community.friendship.dto.FriendshipPageResponse;
import com.example.genz.community.friendship.dto.SendFriendRequestRequest;
import com.example.genz.community.friendship.enums.FriendRequestStatus;
import com.example.genz.community.friendship.service.BlockUserResult;
import com.example.genz.community.friendship.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/friend-requests")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication
    ) {
        FriendRequestResponse response = friendshipService.sendRequest(
                request.userId(),
                authentication.getName()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Friend request sent", response));
    }

    @GetMapping("/friend-requests/incoming")
    public ResponseEntity<ApiResponse<FriendshipPageResponse<FriendRequestResponse>>> getIncomingRequests(
            @RequestParam(defaultValue = "PENDING") FriendRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        FriendshipPageResponse<FriendRequestResponse> response = friendshipService.getIncomingRequests(
                authentication.getName(),
                status,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Incoming friend requests retrieved", response));
    }

    @GetMapping("/friend-requests/outgoing")
    public ResponseEntity<ApiResponse<FriendshipPageResponse<FriendRequestResponse>>> getOutgoingRequests(
            @RequestParam(defaultValue = "PENDING") FriendRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        FriendshipPageResponse<FriendRequestResponse> response = friendshipService.getOutgoingRequests(
                authentication.getName(),
                status,
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Outgoing friend requests retrieved", response));
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        FriendResponse response = friendshipService.acceptRequest(requestId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Friend request accepted", response));
    }

    @PostMapping("/friend-requests/{requestId}/decline")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> declineRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        FriendRequestResponse response = friendshipService.declineRequest(requestId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Friend request declined", response));
    }

    @DeleteMapping("/friend-requests/{requestId}")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> cancelRequest(
            @PathVariable UUID requestId,
            Authentication authentication
    ) {
        FriendRequestResponse response = friendshipService.cancelRequest(requestId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Friend request cancelled", response));
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<FriendshipPageResponse<FriendResponse>>> getFriends(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        FriendshipPageResponse<FriendResponse> response = friendshipService.getFriends(
                authentication.getName(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Friends retrieved", response));
    }

    @DeleteMapping("/friends/{friendUserId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(
            @PathVariable UUID friendUserId,
            Authentication authentication
    ) {
        friendshipService.removeFriend(friendUserId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Friend removed", null));
    }

    @PostMapping("/blocks/{userId}")
    public ResponseEntity<ApiResponse<BlockedUserResponse>> blockUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        BlockUserResult result = friendshipService.blockUser(userId, authentication.getName());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(
                        result.created() ? "User blocked" : "User is already blocked",
                        result.block()
                ));
    }

    @GetMapping("/blocks")
    public ResponseEntity<ApiResponse<FriendshipPageResponse<BlockedUserResponse>>> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Authentication authentication
    ) {
        FriendshipPageResponse<BlockedUserResponse> response = friendshipService.getBlockedUsers(
                authentication.getName(),
                page,
                size
        );
        return ResponseEntity.ok(ApiResponse.success("Blocked users retrieved", response));
    }

    @DeleteMapping("/blocks/{userId}")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        friendshipService.unblockUser(userId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("User unblocked", null));
    }
}
