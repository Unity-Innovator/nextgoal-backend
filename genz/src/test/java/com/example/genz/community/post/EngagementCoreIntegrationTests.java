package com.example.genz.community.post;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.service.FriendshipService;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.EngagementLikeResponse;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostComment;
import com.example.genz.community.post.enums.CommentStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.post.repository.CommentLikeRepository;
import com.example.genz.community.post.repository.PostCommentRepository;
import com.example.genz.community.post.repository.PostLikeRepository;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.community.post.service.EngagementService;
import com.example.genz.community.post.service.PostService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_engagement;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
class EngagementCoreIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void postLikeAndUnlikeAreIdempotentAndKeepCounterConsistent() {
        User alice = saveUser("Alice", "0130000001");
        User bob = saveUser("Bob", "0130000002");
        PostResponse post = createPost(alice, PostVisibility.PUBLIC);

        EngagementLikeResponse firstLike = engagementService.likePost(post.id(), bob.getPhoneNumber());
        EngagementLikeResponse duplicateLike = engagementService.likePost(post.id(), bob.getPhoneNumber());

        assertTrue(firstLike.liked());
        assertTrue(firstLike.changed());
        assertEquals(1, firstLike.likeCount());
        assertFalse(duplicateLike.changed());
        assertEquals(1, duplicateLike.likeCount());
        assertPostLikeCounter(post.id());

        EngagementLikeResponse unlike = engagementService.unlikePost(post.id(), bob.getPhoneNumber());
        EngagementLikeResponse duplicateUnlike = engagementService.unlikePost(post.id(), bob.getPhoneNumber());

        assertFalse(unlike.liked());
        assertTrue(unlike.changed());
        assertEquals(0, unlike.likeCount());
        assertFalse(duplicateUnlike.changed());
        assertPostLikeCounter(post.id());
    }

    @Test
    void commentsRepliesEditsAndSoftDeletesKeepBothCountersConsistent() {
        User alice = saveUser("Alice", "0131000001");
        User bob = saveUser("Bob", "0131000002");
        PostResponse post = createPost(alice, PostVisibility.PUBLIC);

        CommentResponse comment = engagementService.addComment(
                post.id(),
                new CommentRequest("First comment"),
                bob.getPhoneNumber()
        );
        CommentResponse edited = engagementService.editComment(
                comment.id(),
                new CommentRequest("Edited comment"),
                bob.getPhoneNumber()
        );

        assertEquals("Edited comment", edited.content());
        assertThrows(
                AccessDeniedException.class,
                () -> engagementService.editComment(
                        comment.id(),
                        new CommentRequest("Not mine"),
                        alice.getPhoneNumber()
                )
        );

        CommentResponse reply = engagementService.replyToComment(
                comment.id(),
                new CommentRequest("A reply"),
                alice.getPhoneNumber()
        );
        assertEquals(comment.id(), reply.parentCommentId());
        assertCommentCounters(post.id(), comment.id(), 2, 1);

        engagementService.deleteComment(reply.id(), alice.getPhoneNumber());
        assertCommentCounters(post.id(), comment.id(), 1, 0);

        engagementService.deleteComment(comment.id(), bob.getPhoneNumber());
        assertCommentCounters(post.id(), comment.id(), 0, 0);
        PostComment deleted = postCommentRepository.findById(comment.id()).orElseThrow();
        assertEquals(CommentStatus.DELETED, deleted.getStatus());
        assertNotNull(deleted.getDeletedAt());
        assertThrows(
                PostNotFoundException.class,
                () -> engagementService.likeComment(comment.id(), alice.getPhoneNumber())
        );
    }

    @Test
    void commentLikesAreIdempotentAndKeepCounterConsistent() {
        User alice = saveUser("Alice", "0132000001");
        User bob = saveUser("Bob", "0132000002");
        PostResponse post = createPost(alice, PostVisibility.PUBLIC);
        CommentResponse comment = engagementService.addComment(
                post.id(), new CommentRequest("Useful"), alice.getPhoneNumber()
        );

        EngagementLikeResponse firstLike = engagementService.likeComment(
                comment.id(), bob.getPhoneNumber()
        );
        EngagementLikeResponse duplicateLike = engagementService.likeComment(
                comment.id(), bob.getPhoneNumber()
        );

        assertTrue(firstLike.changed());
        assertEquals(1, firstLike.likeCount());
        assertFalse(duplicateLike.changed());
        assertCommentLikeCounter(comment.id());

        engagementService.unlikeComment(comment.id(), bob.getPhoneNumber());
        EngagementLikeResponse duplicateUnlike = engagementService.unlikeComment(
                comment.id(), bob.getPhoneNumber()
        );
        assertFalse(duplicateUnlike.changed());
        assertCommentLikeCounter(comment.id());
    }

    @Test
    void visibilityFriendshipAndBlocksAreAppliedToEngagement() {
        User alice = saveUser("Alice", "0133000001");
        User bob = saveUser("Bob", "0133000002");
        PostResponse privatePost = createPost(alice, PostVisibility.PRIVATE);
        PostResponse friendsPost = createPost(alice, PostVisibility.FRIENDS_ONLY);

        assertThrows(
                PostNotFoundException.class,
                () -> engagementService.likePost(privatePost.id(), bob.getPhoneNumber())
        );
        assertThrows(
                PostNotFoundException.class,
                () -> engagementService.addComment(
                        friendsPost.id(), new CommentRequest("Hidden"), bob.getPhoneNumber()
                )
        );

        makeFriends(alice, bob);
        CommentResponse visibleComment = engagementService.addComment(
                friendsPost.id(), new CommentRequest("Visible now"), bob.getPhoneNumber()
        );
        assertNotNull(visibleComment.id());

        friendshipService.blockUser(bob.getId(), alice.getPhoneNumber());
        assertThrows(
                PostNotFoundException.class,
                () -> engagementService.likePost(friendsPost.id(), bob.getPhoneNumber())
        );
        assertThrows(
                PostNotFoundException.class,
                () -> engagementService.likeComment(visibleComment.id(), bob.getPhoneNumber())
        );
    }

    @Test
    void restEndpointsUseAuthenticatedUserAndExposeChangedState() throws Exception {
        User alice = saveUser("Alice", "0134000001");
        User bob = saveUser("Bob", "0134000002");
        PostResponse post = createPost(alice, PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/community/posts/{postId}/like", post.id())
                        .with(user(bob.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.changed").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        String commentJson = mockMvc.perform(post("/api/community/posts/{postId}/comments", post.id())
                        .with(user(bob.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"REST comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.author.id").value(bob.getId().toString()))
                .andExpect(jsonPath("$.data.ownedByCurrentUser").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode commentData = objectMapper.readTree(commentJson).path("data");
        UUID commentId = UUID.fromString(commentData.path("id").asText());

        mockMvc.perform(put("/api/community/comments/{commentId}", commentId)
                        .with(user(bob.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated through REST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated through REST"));

        mockMvc.perform(post("/api/community/comments/{commentId}/like", commentId)
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(delete("/api/community/comments/{commentId}/like", commentId)
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(0));
    }

    @Test
    void blankCommentsAreRejectedByRestValidation() throws Exception {
        User alice = saveUser("Alice", "0135000001");
        PostResponse post = createPost(alice, PostVisibility.PUBLIC);

        mockMvc.perform(post("/api/community/posts/{postId}/comments", post.id())
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    private void assertPostLikeCounter(UUID postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        assertEquals(postLikeRepository.countByPost_Id(postId), post.getLikeCount());
    }

    private void assertCommentLikeCounter(UUID commentId) {
        PostComment comment = postCommentRepository.findById(commentId).orElseThrow();
        assertEquals(commentLikeRepository.countByComment_Id(commentId), comment.getLikeCount());
    }

    private void assertCommentCounters(
            UUID postId,
            UUID parentCommentId,
            int expectedPostCount,
            int expectedReplyCount
    ) {
        Post post = postRepository.findById(postId).orElseThrow();
        PostComment parent = postCommentRepository.findById(parentCommentId).orElseThrow();
        assertEquals(expectedPostCount, post.getCommentCount());
        assertEquals(expectedReplyCount, parent.getReplyCount());
        assertEquals(
                postCommentRepository.countByPost_IdAndStatus(postId, CommentStatus.ACTIVE),
                post.getCommentCount()
        );
        assertEquals(
                postCommentRepository.countByParentComment_IdAndStatus(
                        parentCommentId,
                        CommentStatus.ACTIVE
                ),
                parent.getReplyCount()
        );
    }

    private PostResponse createPost(User author, PostVisibility visibility) {
        return postService.createPost(
                new CreatePostRequest(
                        PostType.LIFE_UPDATE,
                        "Engagement test post",
                        visibility,
                        null,
                        List.of()
                ),
                author.getPhoneNumber()
        );
    }

    private void makeFriends(User sender, User receiver) {
        FriendRequestResponse request = friendshipService.sendRequest(
                receiver.getId(), sender.getPhoneNumber()
        );
        friendshipService.acceptRequest(request.id(), receiver.getPhoneNumber());
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
