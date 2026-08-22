package com.example.genz.community.friendship;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.dto.FriendResponse;
import com.example.genz.community.friendship.dto.FriendshipPageResponse;
import com.example.genz.community.friendship.enums.FriendRequestStatus;
import com.example.genz.community.friendship.exception.FriendshipBadRequestException;
import com.example.genz.community.friendship.exception.FriendshipConflictException;
import com.example.genz.community.friendship.repository.FriendRequestRepository;
import com.example.genz.community.friendship.repository.FriendshipRepository;
import com.example.genz.community.friendship.repository.UserBlockRepository;
import com.example.genz.community.friendship.service.BlockUserResult;
import com.example.genz.community.friendship.service.FriendshipService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_friendship;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
class FriendshipCoreIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedRestEndpointsUseTheSecurityPrincipalAsTheSender() throws Exception {
        User alice = saveUser("Alice", "0110000001");
        User bob = saveUser("Bob", "0110000002");

        mockMvc.perform(post("/api/community/friend-requests")
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + bob.getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sender.id").value(alice.getId().toString()))
                .andExpect(jsonPath("$.data.receiver.id").value(bob.getId().toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        mockMvc.perform(get("/api/community/friend-requests/outgoing")
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].receiver.id").value(bob.getId().toString()));
    }

    @Test
    void requestLifecycleCreatesOneOrderedFriendshipAndListsItForBothUsers() {
        User alice = saveUser("Alice", "0111000001");
        User bob = saveUser("Bob", "0111000002");

        FriendRequestResponse request = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());

        assertEquals(FriendRequestStatus.PENDING, request.status());
        assertNotNull(request.createdAt());
        assertEquals(1, friendshipService.getOutgoingRequests(
                alice.getPhoneNumber(), FriendRequestStatus.PENDING, 0, 20
        ).totalElements());
        assertEquals(1, friendshipService.getIncomingRequests(
                bob.getPhoneNumber(), FriendRequestStatus.PENDING, 0, 20
        ).totalElements());

        FriendResponse accepted = friendshipService.acceptRequest(request.id(), bob.getPhoneNumber());

        assertEquals(alice.getId(), accepted.user().id());
        assertNotNull(accepted.friendsSince());
        assertEquals(1, friendshipRepository.count());
        assertEquals(1, friendshipService.getFriends(alice.getPhoneNumber(), 0, 20).totalElements());
        assertEquals(1, friendshipService.getFriends(bob.getPhoneNumber(), 0, 20).totalElements());
        assertThrows(
                FriendshipConflictException.class,
                () -> friendshipService.sendRequest(alice.getId(), bob.getPhoneNumber())
        );
    }

    @Test
    void pendingRequestPreventsReverseDuplicateAndOnlyReceiverCanAccept() {
        User alice = saveUser("Alice", "0112000001");
        User bob = saveUser("Bob", "0112000002");
        FriendRequestResponse request = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());

        assertThrows(
                FriendshipConflictException.class,
                () -> friendshipService.sendRequest(alice.getId(), bob.getPhoneNumber())
        );
        assertThrows(
                AccessDeniedException.class,
                () -> friendshipService.acceptRequest(request.id(), alice.getPhoneNumber())
        );

        FriendRequestResponse cancelled = friendshipService.cancelRequest(request.id(), alice.getPhoneNumber());
        assertEquals(FriendRequestStatus.CANCELLED, cancelled.status());
        assertNotNull(cancelled.respondedAt());
    }

    @Test
    void receiverCanDeclineAndCompletedRequestCannotBeProcessedAgain() {
        User alice = saveUser("Alice", "0113000001");
        User bob = saveUser("Bob", "0113000002");
        FriendRequestResponse request = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());

        FriendRequestResponse declined = friendshipService.declineRequest(request.id(), bob.getPhoneNumber());

        assertEquals(FriendRequestStatus.DECLINED, declined.status());
        assertThrows(
                FriendshipConflictException.class,
                () -> friendshipService.acceptRequest(request.id(), bob.getPhoneNumber())
        );
    }

    @Test
    void blockingIsIdempotentRemovesFriendshipAndPreventsNewRequestsUntilUnblocked() {
        User alice = saveUser("Alice", "0114000001");
        User bob = saveUser("Bob", "0114000002");
        FriendRequestResponse request = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());
        friendshipService.acceptRequest(request.id(), bob.getPhoneNumber());

        BlockUserResult firstBlock = friendshipService.blockUser(bob.getId(), alice.getPhoneNumber());
        BlockUserResult retry = friendshipService.blockUser(bob.getId(), alice.getPhoneNumber());

        assertTrue(firstBlock.created());
        assertFalse(retry.created());
        assertEquals(firstBlock.block().blockId(), retry.block().blockId());
        assertEquals(0, friendshipRepository.count());
        assertEquals(1, userBlockRepository.count());
        assertEquals(1, friendshipService.getBlockedUsers(alice.getPhoneNumber(), 0, 20).totalElements());
        assertThrows(
                AccessDeniedException.class,
                () -> friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber())
        );

        friendshipService.unblockUser(bob.getId(), alice.getPhoneNumber());
        FriendRequestResponse newRequest = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());
        assertEquals(FriendRequestStatus.PENDING, newRequest.status());
    }

    @Test
    void blockingCancelsPendingRequestsInEitherDirection() {
        User alice = saveUser("Alice", "0115000001");
        User bob = saveUser("Bob", "0115000002");
        FriendRequestResponse request = friendshipService.sendRequest(alice.getId(), bob.getPhoneNumber());

        friendshipService.blockUser(bob.getId(), alice.getPhoneNumber());

        assertEquals(
                FriendRequestStatus.CANCELLED,
                friendRequestRepository.findById(request.id()).orElseThrow().getStatus()
        );
    }

    @Test
    void removingFriendAndInputValidationAreEnforced() {
        User alice = saveUser("Alice", "0116000001");
        User bob = saveUser("Bob", "0116000002");
        FriendRequestResponse request = friendshipService.sendRequest(bob.getId(), alice.getPhoneNumber());
        friendshipService.acceptRequest(request.id(), bob.getPhoneNumber());

        friendshipService.removeFriend(bob.getId(), alice.getPhoneNumber());

        FriendshipPageResponse<FriendResponse> friends = friendshipService.getFriends(
                alice.getPhoneNumber(), 0, 20
        );
        assertEquals(0, friends.totalElements());
        assertThrows(
                FriendshipBadRequestException.class,
                () -> friendshipService.sendRequest(alice.getId(), alice.getPhoneNumber())
        );
        assertThrows(
                FriendshipBadRequestException.class,
                () -> friendshipService.getFriends(alice.getPhoneNumber(), -1, 20)
        );
        assertThrows(
                FriendshipBadRequestException.class,
                () -> friendshipService.getFriends(alice.getPhoneNumber(), 0, 101)
        );
    }

    private User saveUser(String fullName, String phoneNumber) {
        User user = new User();
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender(Gender.OTHER);
        user.setStatus("ACTIVE");
        return userRepository.saveAndFlush(user);
    }
}
