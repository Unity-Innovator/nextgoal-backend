package com.example.genz.community.notification;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.service.FriendshipService;
import com.example.genz.community.notification.dto.CommunityNotificationResponse;
import com.example.genz.community.notification.dto.NotificationCursorPageResponse;
import com.example.genz.community.notification.enums.NotificationType;
import com.example.genz.community.notification.repository.CommunityNotificationRepository;
import com.example.genz.community.notification.service.CommunityNotificationService;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.dto.SharePostRequest;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.service.EngagementService;
import com.example.genz.community.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_community_notifications;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
class CommunityNotificationIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommunityNotificationRepository notificationRepository;

    @Autowired
    private CommunityNotificationService notificationService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private PostService postService;

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void communityActionsCreateDurableNotificationsWithoutDuplicatesOrSelfNotifications() {
        User alice = saveUser("Alice", "0150000001");
        User bob = saveUser("Bob", "0150000002");

        FriendRequestResponse request = friendshipService.sendRequest(
                alice.getId(),
                bob.getPhoneNumber()
        );
        friendshipService.acceptRequest(request.id(), alice.getPhoneNumber());

        PostResponse post = createPost(alice, "Notification source post");
        engagementService.likePost(post.id(), bob.getPhoneNumber());
        engagementService.likePost(post.id(), bob.getPhoneNumber());
        engagementService.likePost(post.id(), alice.getPhoneNumber());

        CommentResponse comment = engagementService.addComment(
                post.id(),
                new CommentRequest("Bob's comment"),
                bob.getPhoneNumber()
        );
        engagementService.replyToComment(
                comment.id(),
                new CommentRequest("Alice's reply"),
                alice.getPhoneNumber()
        );
        postService.sharePost(
                post.id(),
                new SharePostRequest("Sharing", PostVisibility.PUBLIC),
                bob.getPhoneNumber()
        );

        List<CommunityNotificationResponse> aliceNotifications = notificationService.getNotifications(
                alice.getPhoneNumber(), false, null, null, 20
        ).items();
        List<CommunityNotificationResponse> bobNotifications = notificationService.getNotifications(
                bob.getPhoneNumber(), false, null, null, 20
        ).items();

        assertEquals(4, aliceNotifications.size());
        assertEquals(
                Set.of(
                        NotificationType.FRIEND_REQUEST,
                        NotificationType.POST_LIKED,
                        NotificationType.POST_COMMENTED,
                        NotificationType.POST_SHARED
                ),
                notificationTypes(aliceNotifications)
        );
        assertEquals(2, bobNotifications.size());
        assertEquals(
                Set.of(NotificationType.FRIEND_ACCEPTED, NotificationType.COMMENT_REPLIED),
                notificationTypes(bobNotifications)
        );
        assertEquals(6, notificationRepository.count());
        assertTrue(aliceNotifications.stream().allMatch(notification -> notification.actor() != null));
        assertTrue(aliceNotifications.stream().allMatch(notification -> !notification.read()));
    }

    @Test
    void notificationCursorUnreadCountAndReadEndpointsAreIdempotentAndOwnerScoped() throws Exception {
        User alice = saveUser("Alice", "0151000001");
        User bob = saveUser("Bob", "0151000002");
        for (int index = 0; index < 3; index++) {
            PostResponse post = createPost(alice, "Post " + index);
            engagementService.likePost(post.id(), bob.getPhoneNumber());
        }

        NotificationCursorPageResponse firstPage = notificationService.getNotifications(
                alice.getPhoneNumber(), true, null, null, 2
        );
        assertEquals(2, firstPage.items().size());
        assertTrue(firstPage.hasNext());
        assertNotNull(firstPage.nextCursor());

        NotificationCursorPageResponse lastPage = notificationService.getNotifications(
                alice.getPhoneNumber(),
                true,
                firstPage.nextCursor().createdAt(),
                firstPage.nextCursor().notificationId(),
                2
        );
        assertEquals(1, lastPage.items().size());
        assertFalse(lastPage.hasNext());
        assertEquals(3, notificationService.getUnreadCount(alice.getPhoneNumber()).unreadCount());

        UUID notificationId = firstPage.items().getFirst().id();
        mockMvc.perform(post("/api/community/notifications/{notificationId}/read", notificationId)
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true))
                .andExpect(jsonPath("$.data.readAt").isNotEmpty());

        mockMvc.perform(post("/api/community/notifications/{notificationId}/read", notificationId)
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk());
        assertEquals(2, notificationService.getUnreadCount(alice.getPhoneNumber()).unreadCount());

        mockMvc.perform(post("/api/community/notifications/{notificationId}/read", notificationId)
                        .with(user(bob.getPhoneNumber())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/community/notifications")
                        .with(user(alice.getPhoneNumber()))
                        .param("unreadOnly", "true")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        mockMvc.perform(post("/api/community/notifications/read-all")
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.markedReadCount").value(2))
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        mockMvc.perform(get("/api/community/notifications/unread-count")
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    private Set<NotificationType> notificationTypes(List<CommunityNotificationResponse> notifications) {
        Set<NotificationType> types = new HashSet<>();
        notifications.forEach(notification -> types.add(notification.type()));
        return types;
    }

    private PostResponse createPost(User author, String content) {
        return postService.createPost(
                new CreatePostRequest(
                        PostType.LIFE_UPDATE,
                        content,
                        PostVisibility.PUBLIC,
                        null,
                        List.of()
                ),
                author.getPhoneNumber()
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
