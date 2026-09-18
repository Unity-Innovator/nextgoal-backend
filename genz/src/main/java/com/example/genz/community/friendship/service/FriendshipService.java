package com.example.genz.community.friendship.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.BlockedUserResponse;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.dto.FriendResponse;
import com.example.genz.community.friendship.dto.FriendshipUserResponse;
import com.example.genz.community.friendship.dto.FriendshipPageResponse;
import com.example.genz.community.friendship.entity.FriendRequest;
import com.example.genz.community.friendship.entity.Friendship;
import com.example.genz.community.friendship.entity.UserBlock;
import com.example.genz.community.friendship.enums.FriendRequestStatus;
import com.example.genz.community.friendship.exception.FriendshipBadRequestException;
import com.example.genz.community.friendship.exception.FriendshipConflictException;
import com.example.genz.community.friendship.exception.FriendshipNotFoundException;
import com.example.genz.community.friendship.mapper.FriendshipMapper;
import com.example.genz.community.friendship.repository.FriendRequestRepository;
import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.community.friendship.repository.UserBlockRepository;
import com.example.genz.community.notification.service.CommunityNotificationService;
import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.service.CommunityRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipMapper mapper;
    private final CommunityNotificationService notificationService;
    private final CommunityRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public FriendshipPageResponse<FriendshipUserResponse> searchUsers(
            String query, String currentPhoneNumber, int page, int size) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        String term = query == null ? "" : query.trim();
        if (term.length() < 2 || term.length() > 100) {
            throw new FriendshipBadRequestException("Search must contain between 2 and 100 characters");
        }
        String pattern = "%" + term.replace("!", "!!").replace("%", "!%")
                .replace("_", "!_") + "%";
        return FriendshipPageResponse.from(userRepository.searchFriendCandidates(
                currentUser.getId(), pattern, term, pageRequest(page, size))
                .map(user -> new FriendshipUserResponse(
                        user.getId(), user.getFullName())));
    }

    @Transactional
    public FriendRequestResponse sendRequest(UUID receiverId, String currentPhoneNumber) {
        User sender = requireCurrentUser(currentPhoneNumber);
        User receiver = requireUser(receiverId);
        requireDifferentUsers(sender.getId(), receiver.getId());
        rateLimiter.check(sender.getId(), CommunityAction.FRIEND_REQUEST);

        if (isBlockedEitherWay(sender.getId(), receiver.getId())) {
            throw new AccessDeniedException("A friend request cannot be sent between blocked users");
        }

        UserPair pair = orderedPair(sender, receiver);
        if (friendshipRepository.existsByUserOne_IdAndUserTwo_Id(pair.first().getId(), pair.second().getId())) {
            throw new FriendshipConflictException("You are already friends with this user");
        }

        if (hasPendingRequest(sender.getId(), receiver.getId())) {
            throw new FriendshipConflictException("A pending friend request already exists between these users");
        }

        FriendRequest request = new FriendRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(FriendRequestStatus.PENDING);

        FriendRequest savedRequest;
        try {
            savedRequest = friendRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException exception) {
            throw new FriendshipConflictException("A pending friend request already exists between these users");
        }
        notificationService.notifyFriendRequest(receiver, sender, savedRequest.getId());
        return mapper.toFriendRequestResponse(savedRequest);
    }

    @Transactional(readOnly = true)
    public FriendshipPageResponse<FriendRequestResponse> getIncomingRequests(
            String currentPhoneNumber,
            FriendRequestStatus status,
            int page,
            int size
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Page<FriendRequestResponse> requests = friendRequestRepository
                .findByReceiver_IdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        statusOrPending(status),
                        pageRequest(page, size)
                )
                .map(mapper::toFriendRequestResponse);
        return FriendshipPageResponse.from(requests);
    }

    @Transactional(readOnly = true)
    public FriendshipPageResponse<FriendRequestResponse> getOutgoingRequests(
            String currentPhoneNumber,
            FriendRequestStatus status,
            int page,
            int size
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Page<FriendRequestResponse> requests = friendRequestRepository
                .findBySender_IdAndStatusOrderByCreatedAtDesc(
                        currentUser.getId(),
                        statusOrPending(status),
                        pageRequest(page, size)
                )
                .map(mapper::toFriendRequestResponse);
        return FriendshipPageResponse.from(requests);
    }

    @Transactional
    public FriendResponse acceptRequest(UUID requestId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        FriendRequest request = requireRequestForUpdate(requestId);
        requireReceiver(request, currentUser);
        requirePending(request);

        if (isBlockedEitherWay(request.getSender().getId(), request.getReceiver().getId())) {
            throw new AccessDeniedException("A friend request cannot be accepted between blocked users");
        }

        UserPair pair = orderedPair(request.getSender(), request.getReceiver());
        if (friendshipRepository.existsByUserOne_IdAndUserTwo_Id(pair.first().getId(), pair.second().getId())) {
            throw new FriendshipConflictException("These users are already friends");
        }

        request.setStatus(FriendRequestStatus.ACCEPTED);
        request.setRespondedAt(Instant.now());
        friendRequestRepository.saveAndFlush(request);

        Friendship friendship = new Friendship();
        friendship.setUserOne(pair.first());
        friendship.setUserTwo(pair.second());
        friendship.setRequest(request);

        Friendship savedFriendship;
        try {
            savedFriendship = friendshipRepository.saveAndFlush(friendship);
        } catch (DataIntegrityViolationException exception) {
            throw new FriendshipConflictException("These users are already friends");
        }
        notificationService.notifyFriendAccepted(
                request.getSender(),
                currentUser,
                request.getId()
        );
        return mapper.toFriendResponse(savedFriendship, currentUser.getId());
    }

    @Transactional
    public FriendRequestResponse declineRequest(UUID requestId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        FriendRequest request = requireRequestForUpdate(requestId);
        requireReceiver(request, currentUser);
        requirePending(request);
        return completeRequest(request, FriendRequestStatus.DECLINED);
    }

    @Transactional
    public FriendRequestResponse cancelRequest(UUID requestId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        FriendRequest request = requireRequestForUpdate(requestId);
        if (!request.getSender().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the sender can cancel this friend request");
        }
        requirePending(request);
        return completeRequest(request, FriendRequestStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public FriendshipPageResponse<FriendResponse> getFriends(
            String currentPhoneNumber,
            int page,
            int size
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Page<FriendResponse> friends = friendshipRepository
                .findByUserOne_IdOrUserTwo_IdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        currentUser.getId(),
                        pageRequest(page, size)
                )
                .map(friendship -> mapper.toFriendResponse(friendship, currentUser.getId()));
        return FriendshipPageResponse.from(friends);
    }

    @Transactional
    public void removeFriend(UUID friendUserId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        requireDifferentUsers(currentUser.getId(), friendUserId);
        IdPair pair = orderedIds(currentUser.getId(), friendUserId);
        Friendship friendship = friendshipRepository
                .findByUserOne_IdAndUserTwo_Id(pair.first(), pair.second())
                .orElseThrow(() -> new FriendshipNotFoundException("Friendship not found"));
        friendshipRepository.delete(friendship);
        friendshipRepository.flush();
    }

    @Transactional
    public BlockUserResult blockUser(UUID blockedUserId, String currentPhoneNumber) {
        User blocker = requireCurrentUser(currentPhoneNumber);
        User blockedUser = requireUser(blockedUserId);
        requireDifferentUsers(blocker.getId(), blockedUser.getId());

        IdPair pair = orderedIds(blocker.getId(), blockedUser.getId());
        friendshipRepository.findByUserOne_IdAndUserTwo_Id(pair.first(), pair.second())
                .ifPresent(friendshipRepository::delete);
        friendRequestRepository.cancelPendingBetween(blocker.getId(), blockedUser.getId(), Instant.now());

        return userBlockRepository.findByBlocker_IdAndBlockedUser_Id(blocker.getId(), blockedUser.getId())
                .map(existing -> new BlockUserResult(mapper.toBlockedUserResponse(existing), false))
                .orElseGet(() -> createBlock(blocker, blockedUser));
    }

    @Transactional(readOnly = true)
    public FriendshipPageResponse<BlockedUserResponse> getBlockedUsers(
            String currentPhoneNumber,
            int page,
            int size
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Page<BlockedUserResponse> blocks = userBlockRepository
                .findByBlocker_IdOrderByCreatedAtDesc(currentUser.getId(), pageRequest(page, size))
                .map(mapper::toBlockedUserResponse);
        return FriendshipPageResponse.from(blocks);
    }

    @Transactional
    public void unblockUser(UUID blockedUserId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        requireDifferentUsers(currentUser.getId(), blockedUserId);
        userBlockRepository.findByBlocker_IdAndBlockedUser_Id(currentUser.getId(), blockedUserId)
                .ifPresent(userBlockRepository::delete);
        userBlockRepository.flush();
    }

    private FriendRequestResponse completeRequest(
            FriendRequest request,
            FriendRequestStatus completedStatus
    ) {
        request.setStatus(completedStatus);
        request.setRespondedAt(Instant.now());
        return mapper.toFriendRequestResponse(friendRequestRepository.saveAndFlush(request));
    }

    private BlockUserResult createBlock(User blocker, User blockedUser) {
        UserBlock block = new UserBlock();
        block.setBlocker(blocker);
        block.setBlockedUser(blockedUser);
        try {
            return new BlockUserResult(
                    mapper.toBlockedUserResponse(userBlockRepository.saveAndFlush(block)),
                    true
            );
        } catch (DataIntegrityViolationException exception) {
            throw new FriendshipConflictException("This user is already blocked");
        }
    }

    private FriendRequest requireRequestForUpdate(UUID requestId) {
        return friendRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new FriendshipNotFoundException("Friend request not found"));
    }

    private void requirePending(FriendRequest request) {
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw new FriendshipConflictException("This friend request is no longer pending");
        }
    }

    private void requireReceiver(FriendRequest request, User currentUser) {
        if (!request.getReceiver().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the receiver can respond to this friend request");
        }
    }

    private boolean hasPendingRequest(UUID firstUserId, UUID secondUserId) {
        return friendRequestRepository.existsBySender_IdAndReceiver_IdAndStatus(
                firstUserId,
                secondUserId,
                FriendRequestStatus.PENDING
        ) || friendRequestRepository.existsBySender_IdAndReceiver_IdAndStatus(
                secondUserId,
                firstUserId,
                FriendRequestStatus.PENDING
        );
    }

    private boolean isBlockedEitherWay(UUID firstUserId, UUID secondUserId) {
        return userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(firstUserId, secondUserId)
                || userBlockRepository.existsByBlocker_IdAndBlockedUser_Id(secondUserId, firstUserId);
    }

    private User requireCurrentUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new FriendshipNotFoundException("Authenticated user was not found"));
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new FriendshipNotFoundException("User not found: " + userId));
    }

    private void requireDifferentUsers(UUID firstUserId, UUID secondUserId) {
        if (firstUserId.equals(secondUserId)) {
            throw new FriendshipBadRequestException("You cannot perform this action on yourself");
        }
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new FriendshipBadRequestException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new FriendshipBadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size);
    }

    private FriendRequestStatus statusOrPending(FriendRequestStatus status) {
        return status == null ? FriendRequestStatus.PENDING : status;
    }

    private UserPair orderedPair(User first, User second) {
        return first.getId().compareTo(second.getId()) < 0
                ? new UserPair(first, second)
                : new UserPair(second, first);
    }

    private IdPair orderedIds(UUID first, UUID second) {
        return first.compareTo(second) < 0
                ? new IdPair(first, second)
                : new IdPair(second, first);
    }

    private record UserPair(User first, User second) {
    }

    private record IdPair(UUID first, UUID second) {
    }
}
