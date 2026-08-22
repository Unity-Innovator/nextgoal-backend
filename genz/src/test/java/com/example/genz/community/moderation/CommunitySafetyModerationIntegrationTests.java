package com.example.genz.community.moderation;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.service.FriendshipService;
import com.example.genz.community.moderation.dto.CreateReportRequest;
import com.example.genz.community.moderation.dto.ModerationReportResponse;
import com.example.genz.community.moderation.dto.ReviewReportRequest;
import com.example.genz.community.moderation.exception.ModerationConflictException;
import com.example.genz.community.moderation.service.CommunityModerationService;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.PostMediaRequest;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostComment;
import com.example.genz.community.post.enums.CommentStatus;
import com.example.genz.community.post.enums.PostMediaType;
import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.enums.ReportReason;
import com.example.genz.community.post.enums.ReportStatus;
import com.example.genz.community.post.exception.PostBadRequestException;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.post.repository.PostCommentRepository;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.community.post.service.EngagementService;
import com.example.genz.community.post.service.PostService;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.repository.UserSettingsRepository;
import com.example.genz.transaction.enums.Currency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_moderation;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
class CommunitySafetyModerationIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository settingsRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostCommentRepository commentRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private CommunityModerationService moderationService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void usersCanReportPostsAndCommentsAndAdminsCanReviewAndRemoveThem() throws Exception {
        User author = saveUser("Author", "0160000001", UserRole.USER);
        User reporter = saveUser("Reporter", "0160000002", UserRole.USER);
        User admin = saveUser("Admin", "0160000003", UserRole.ADMIN);
        PostResponse post = createPost(author, "Reported post", PostVisibility.PUBLIC);
        CommentResponse comment = engagementService.addComment(
                post.id(), new CommentRequest("Reported comment"), author.getPhoneNumber()
        );

        ModerationReportResponse postReport = moderationService.reportPost(
                post.id(),
                new CreateReportRequest(ReportReason.SPAM, "Repeated promotion"),
                reporter.getPhoneNumber()
        );
        ModerationReportResponse commentReport = moderationService.reportComment(
                comment.id(),
                new CreateReportRequest(ReportReason.HARASSMENT, "Personal attack"),
                reporter.getPhoneNumber()
        );

        assertEquals(ReportStatus.PENDING, postReport.status());
        assertEquals(ReportStatus.PENDING, commentReport.status());
        assertThrows(
                ModerationConflictException.class,
                () -> moderationService.reportPost(
                        post.id(),
                        new CreateReportRequest(ReportReason.OTHER, null),
                        reporter.getPhoneNumber()
                )
        );

        mockMvc.perform(get("/api/community/admin/reports/posts")
                        .with(user(reporter.getPhoneNumber()).roles("USER")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/community/admin/reports/posts")
                        .with(user(admin.getPhoneNumber()).roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].reason").value("SPAM"));

        mockMvc.perform(put("/api/community/admin/reports/posts/{reportId}", postReport.id())
                        .with(user(admin.getPhoneNumber()).roles("ADMIN"))
                        .contentType("application/json")
                        .content("{\"status\":\"ACTION_TAKEN\",\"moderationNote\":\"Removed spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTION_TAKEN"))
                .andExpect(jsonPath("$.data.reviewedBy.id").value(admin.getId().toString()));

        moderationService.reviewCommentReport(
                commentReport.id(),
                new ReviewReportRequest(ReportStatus.ACTION_TAKEN, "Removed harassment"),
                admin.getPhoneNumber()
        );

        Post removedPost = postRepository.findById(post.id()).orElseThrow();
        PostComment removedComment = commentRepository.findById(comment.id()).orElseThrow();
        assertEquals(PostStatus.HIDDEN, removedPost.getStatus());
        assertEquals(CommentStatus.HIDDEN, removedComment.getStatus());
        assertNotNull(removedPost.getDeletedAt());
        assertNotNull(removedComment.getDeletedAt());
    }

    @Test
    void profileVisibilityAndCommentSettingsAreEnforced() {
        User alice = saveUser("Alice", "0161000001", UserRole.USER);
        User bob = saveUser("Bob", "0161000002", UserRole.USER);
        saveSettings(alice, PostVisibility.FRIENDS_ONLY, false);

        assertThrows(
                PostBadRequestException.class,
                () -> createPost(alice, "Too public", PostVisibility.PUBLIC)
        );
        PostResponse friendsPost = createPost(alice, "Friends only", PostVisibility.FRIENDS_ONLY);

        assertThrows(
                PostNotFoundException.class,
                () -> postService.getPost(friendsPost.id(), bob.getPhoneNumber())
        );
        assertEquals(0, postService.getUserPosts(
                alice.getId(), bob.getPhoneNumber(), null, null, 20
        ).items().size());

        FriendRequestResponse request = friendshipService.sendRequest(
                bob.getId(), alice.getPhoneNumber()
        );
        friendshipService.acceptRequest(request.id(), bob.getPhoneNumber());
        PostResponse visiblePost = postService.getPost(friendsPost.id(), bob.getPhoneNumber());
        assertEquals(friendsPost.id(), visiblePost.id());
        assertEquals(null, visiblePost.author().fullName());
        assertThrows(
                AccessDeniedException.class,
                () -> engagementService.addComment(
                        friendsPost.id(), new CommentRequest("Not allowed"), bob.getPhoneNumber()
                )
        );
        assertNotNull(engagementService.addComment(
                friendsPost.id(), new CommentRequest("Owner comment"), alice.getPhoneNumber()
        ).id());
    }

    @Test
    void serviceLevelValidationRejectsUnsafeTextAndMediaSchemes() {
        User alice = saveUser("Alice", "0162000001", UserRole.USER);

        assertThrows(
                PostBadRequestException.class,
                () -> createPost(alice, "<script>alert(1)</script>", PostVisibility.PUBLIC)
        );
        assertThrows(
                PostBadRequestException.class,
                () -> postService.createPost(
                        new CreatePostRequest(
                                PostType.LIFE_UPDATE,
                                null,
                                PostVisibility.PUBLIC,
                                null,
                                List.of(new PostMediaRequest(
                                        PostMediaType.IMAGE,
                                        "javascript:alert(1)",
                                        null,
                                        null,
                                        null,
                                        null
                                ))
                        ),
                        alice.getPhoneNumber()
                )
        );
    }

    private PostResponse createPost(User author, String content, PostVisibility visibility) {
        return postService.createPost(
                new CreatePostRequest(PostType.LIFE_UPDATE, content, visibility, null, List.of()),
                author.getPhoneNumber()
        );
    }

    private User saveUser(String fullName, String phoneNumber, UserRole role) {
        User user = new User();
        user.setFullName(fullName);
        user.setPhoneNumber(phoneNumber);
        user.setDateOfBirth(LocalDate.of(2000, 1, 1));
        user.setGender(Gender.OTHER);
        user.setStatus("ACTIVE");
        user.setRole(role);
        return userRepository.saveAndFlush(user);
    }

    private void saveSettings(User user, PostVisibility visibility, boolean allowComments) {
        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settings.setLanguage("en");
        settings.setTheme("SYSTEM");
        settings.setBillReminder(false);
        settings.setGoalAlert(false);
        settings.setLessonReminder(false);
        settings.setAiTips(false);
        settings.setFaceIdEnabled(false);
        settings.setAutoLockMinutes(5);
        settings.setPushNotificationsEnabled(true);
        settings.setSmsNotificationsEnabled(false);
        settings.setEmailNotificationsEnabled(false);
        settings.setProfileVisibility(visibility);
        settings.setShowSavingsGoalsPublicly(false);
        settings.setShowFullNameOnPosts(false);
        settings.setAllowCommentsOnPosts(allowComments);
        settings.setPreferredCurrency(Currency.USD);
        settingsRepository.saveAndFlush(settings);
    }
}
