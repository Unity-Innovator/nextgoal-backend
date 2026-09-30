package com.example.genz.community.post;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.friendship.dto.FriendRequestResponse;
import com.example.genz.community.friendship.service.FriendshipService;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.PostCursorPageResponse;
import com.example.genz.community.post.dto.PostMediaRequest;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.dto.SharePostRequest;
import com.example.genz.community.post.dto.UpdatePostRequest;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.enums.PostMediaType;
import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.exception.PostBadRequestException;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.community.post.service.PostService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_posts;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@AutoConfigureMockMvc
@Transactional
public class PostCoreIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private FriendshipService friendshipService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void restApiCreatesRetrievesAndUpdatesPostMediaUsingAuthenticatedOwner() throws Exception {
        User alice = saveUser("Alice", "0120000001");

        String response = mockMvc.perform(post("/api/community/posts")
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postType": "LIFE_UPDATE",
                                  "content": "My first post",
                                  "visibility": "PUBLIC",
                                  "media": [{
                                    "mediaType": "IMAGE",
                                    "mediaUrl": "https://cdn.example.com/photo.jpg",
                                    "thumbnailUrl": "https://cdn.example.com/thumb.jpg",
                                    "widthPx": 1080,
                                    "heightPx": 720
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.author.id").value(alice.getId().toString()))
                .andExpect(jsonPath("$.data.ownedByCurrentUser").value(true))
                .andExpect(jsonPath("$.data.media[0].sortOrder").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String postId = objectMapper.readTree(response).path("data").path("id").asText();

        mockMvc.perform(get("/api/community/posts/{postId}", postId)
                        .with(user(alice.getPhoneNumber())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("My first post"))
                .andExpect(jsonPath("$.data.media[0].mediaType").value("IMAGE"));

        mockMvc.perform(put("/api/community/posts/{postId}", postId)
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Updated post",
                                  "visibility": "PRIVATE",
                                  "media": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated post"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.media.length()").value(0));
    }

    @Test
    void onlyOwnerCanUpdateOrDeleteAndDeletionIsSoft() {
        User alice = saveUser("Alice", "0121000001");
        User bob = saveUser("Bob", "0121000002");
        PostResponse created = createTextPost(alice, "Owner content", PostVisibility.PUBLIC);

        assertThrows(
                AccessDeniedException.class,
                () -> postService.updatePost(
                        created.id(),
                        new UpdatePostRequest("Stolen", null, null),
                        bob.getPhoneNumber()
                )
        );
        assertThrows(
                AccessDeniedException.class,
                () -> postService.deletePost(created.id(), bob.getPhoneNumber())
        );

        postService.deletePost(created.id(), alice.getPhoneNumber());

        Post deleted = postRepository.findById(created.id()).orElseThrow();
        assertEquals(PostStatus.DELETED, deleted.getStatus());
        assertNotNull(deleted.getDeletedAt());
        assertThrows(
                PostNotFoundException.class,
                () -> postService.getPost(created.id(), alice.getPhoneNumber())
        );
    }

    @Test
    void feedAndUserPostsEnforcePublicFriendsAndPrivateVisibility() {
        User alice = saveUser("Alice", "0122000001");
        User bob = saveUser("Bob", "0122000002");
        User charlie = saveUser("Charlie", "0122000003");
        makeFriends(alice, bob);

        PostResponse publicPost = createTextPost(alice, "Public", PostVisibility.PUBLIC);
        PostResponse friendsPost = createTextPost(alice, "Friends", PostVisibility.FRIENDS_ONLY);
        PostResponse privatePost = createTextPost(alice, "Private", PostVisibility.PRIVATE);

        assertEquals(3, postService.getUserPosts(
                alice.getId(), alice.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(1, postService.getPublicFeed(
                bob.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(2, postService.getFriendsFeed(
                bob.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(1, postService.getPublicFeed(
                charlie.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(0, postService.getFriendsFeed(
                charlie.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(2, postService.getUserPosts(
                alice.getId(), bob.getPhoneNumber(), null, null, 20
        ).items().size());
        assertEquals(publicPost.id(), postService.getPost(publicPost.id(), charlie.getPhoneNumber()).id());
        assertEquals(friendsPost.id(), postService.getPost(friendsPost.id(), bob.getPhoneNumber()).id());
        assertThrows(
                PostNotFoundException.class,
                () -> postService.getPost(friendsPost.id(), charlie.getPhoneNumber())
        );
        assertThrows(
                PostNotFoundException.class,
                () -> postService.getPost(privatePost.id(), bob.getPhoneNumber())
        );
    }

    @Test
    void blockingHidesEvenPublicPostsFromDirectRetrievalAndFeeds() {
        User alice = saveUser("Alice", "0123000001");
        User bob = saveUser("Bob", "0123000002");
        PostResponse publicPost = createTextPost(alice, "Public", PostVisibility.PUBLIC);

        friendshipService.blockUser(alice.getId(), bob.getPhoneNumber());

        assertThrows(
                PostNotFoundException.class,
                () -> postService.getPost(publicPost.id(), bob.getPhoneNumber())
        );
        assertEquals(0, postService.getPublicFeed(
                bob.getPhoneNumber(), null, null, 20
        ).items().size());
    }

    @Test
    void sharingTracksCountsAndPreventsVisibilityEscalation() {
        User alice = saveUser("Alice", "0124000001");
        User bob = saveUser("Bob", "0124000002");
        makeFriends(alice, bob);
        PostResponse publicPost = createTextPost(alice, "Public", PostVisibility.PUBLIC);
        PostResponse friendsPost = createTextPost(alice, "Friends", PostVisibility.FRIENDS_ONLY);
        PostResponse privatePost = createTextPost(alice, "Private", PostVisibility.PRIVATE);

        PostResponse share = postService.sharePost(
                publicPost.id(),
                new SharePostRequest("Worth reading", PostVisibility.PUBLIC),
                bob.getPhoneNumber()
        );

        assertEquals(PostType.SHARED_POST, share.postType());
        assertEquals(publicPost.id(), share.sharedPostId());
        assertEquals(1, postRepository.findById(publicPost.id()).orElseThrow().getShareCount());
        assertThrows(
                PostBadRequestException.class,
                () -> postService.sharePost(
                        friendsPost.id(),
                        new SharePostRequest(null, PostVisibility.PUBLIC),
                        bob.getPhoneNumber()
                )
        );
        assertThrows(
                PostBadRequestException.class,
                () -> postService.sharePost(
                        privatePost.id(),
                        new SharePostRequest(null, PostVisibility.PRIVATE),
                        alice.getPhoneNumber()
                )
        );

        postService.deletePost(share.id(), bob.getPhoneNumber());
        assertEquals(0, postRepository.findById(publicPost.id()).orElseThrow().getShareCount());
    }

    @Test
    void postBodyTypeAndPaginationValidationAreEnforced() throws Exception {
        User alice = saveUser("Alice", "0125000001");

        assertThrows(
                PostBadRequestException.class,
                () -> postService.createPost(
                        new CreatePostRequest(
                                PostType.LIFE_UPDATE,
                                "   ",
                                PostVisibility.PUBLIC,
                                null,
                                List.of()
                        ),
                        alice.getPhoneNumber()
                )
        );
        assertThrows(
                PostBadRequestException.class,
                () -> postService.createPost(
                        new CreatePostRequest(
                                PostType.SHARED_POST,
                                "Invalid direct share",
                                PostVisibility.PUBLIC,
                                null,
                                List.of()
                        ),
                        alice.getPhoneNumber()
                )
        );
        assertThrows(
                PostBadRequestException.class,
                () -> postService.getPublicFeed(alice.getPhoneNumber(), null, null, 101)
        );

        mockMvc.perform(post("/api/community/posts")
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postType": "LIFE_UPDATE",
                                  "media": [{"mediaType": "IMAGE", "mediaUrl": ""}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/community/posts")
                        .with(user(alice.getPhoneNumber()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "postType": "NOT_A_POST_TYPE",
                                  "content": "Invalid enum"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void publicFeedUsesCreatedAtAndPostIdCursorWithoutDuplicates() throws Exception {
        User alice = saveUser("Alice", "0126000001");
        User viewer = saveUser("Viewer", "0126000002");
        for (int index = 0; index < 5; index++) {
            createTextPost(alice, "Post " + index, PostVisibility.PUBLIC);
        }

        PostCursorPageResponse firstPage = postService.getPublicFeed(
                viewer.getPhoneNumber(), null, null, 2
        );
        assertEquals(2, firstPage.items().size());
        assertTrue(firstPage.hasNext());
        assertNotNull(firstPage.nextCursor());

        PostCursorPageResponse secondPage = postService.getPublicFeed(
                viewer.getPhoneNumber(),
                firstPage.nextCursor().createdAt(),
                firstPage.nextCursor().postId(),
                2
        );
        assertEquals(2, secondPage.items().size());
        assertTrue(secondPage.hasNext());

        PostCursorPageResponse lastPage = postService.getPublicFeed(
                viewer.getPhoneNumber(),
                secondPage.nextCursor().createdAt(),
                secondPage.nextCursor().postId(),
                2
        );
        assertEquals(1, lastPage.items().size());
        assertEquals(false, lastPage.hasNext());
        assertEquals(null, lastPage.nextCursor());

        Set<Object> postIds = new HashSet<>();
        firstPage.items().forEach(post -> postIds.add(post.id()));
        secondPage.items().forEach(post -> postIds.add(post.id()));
        lastPage.items().forEach(post -> postIds.add(post.id()));
        assertEquals(5, postIds.size());

        mockMvc.perform(get("/api/community/feed/public")
                        .with(user(viewer.getPhoneNumber()))
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.data.nextCursor.postId").isNotEmpty());

        mockMvc.perform(get("/api/community/feed/public")
                        .with(user(viewer.getPhoneNumber()))
                        .param("cursorCreatedAt", firstPage.nextCursor().createdAt().toString())
                        .param("cursorPostId", firstPage.nextCursor().postId().toString())
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].id")
                        .value(secondPage.items().getFirst().id().toString()));
    }

    @Test
    void cursorFieldsMustBeProvidedTogetherAndDeletedPostsStayOutOfFeeds() {
        User alice = saveUser("Alice", "0127000001");
        User viewer = saveUser("Viewer", "0127000002");
        PostResponse deleted = createTextPost(alice, "Deleted", PostVisibility.PUBLIC);
        PostResponse active = createTextPost(alice, "Active", PostVisibility.PUBLIC);
        postService.deletePost(deleted.id(), alice.getPhoneNumber());

        PostCursorPageResponse feed = postService.getPublicFeed(
                viewer.getPhoneNumber(), null, null, 20
        );

        assertEquals(1, feed.items().size());
        assertEquals(active.id(), feed.items().getFirst().id());
        assertThrows(
                PostBadRequestException.class,
                () -> postService.getPublicFeed(
                        viewer.getPhoneNumber(), active.createdAt(), null, 20
                )
        );
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    public static String md5(String value) {
        return org.springframework.util.DigestUtils.md5DigestAsHex(
                value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void randomFeedUsesStableSeedAndCursorAcrossPagesAndRefreshes() throws Exception {
        jdbc.execute("CREATE ALIAS IF NOT EXISTS MD5 FOR \"com.example.genz.community.post.PostCoreIntegrationTests.md5\"");
        User author = saveUser("Random author", "0128000001");
        User viewer = saveUser("Random viewer", "0128000002");
        Set<java.util.UUID> expected = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            expected.add(createTextPost(author, "Random " + i, PostVisibility.PUBLIC).id());
        }
        createTextPost(author, "Private", PostVisibility.PRIVATE);
        PostResponse removed = createTextPost(author, "Removed", PostVisibility.PUBLIC);
        postService.deletePost(removed.id(), author.getPhoneNumber());
        postRepository.flush();
        PostCursorPageResponse page = postService.getRandomPublicFeed(viewer.getPhoneNumber(), null, null, null, 2);
        var firstCursor = page.nextCursor();
        assertNotNull(firstCursor.randomSeed());
        assertNotNull(firstCursor.snapshot());
        assertEquals(page.items().stream().map(PostResponse::id).toList(),
                postService.getRandomPublicFeed(viewer.getPhoneNumber(), firstCursor.randomSeed(),
                        firstCursor.snapshot(), null, 2).items().stream().map(PostResponse::id).toList());
        // Posts created during a paging session wait until the next refresh.
        PostResponse later = createTextPost(author, "Later", PostVisibility.PUBLIC);
        Post laterEntity = postRepository.findById(later.id()).orElseThrow();
        laterEntity.setCreatedAt(firstCursor.snapshot().plusSeconds(1));
        postRepository.saveAndFlush(laterEntity);
        Set<java.util.UUID> seen = new HashSet<>();
        while (true) {
            for (var item : page.items()) assertTrue(seen.add(item.id()), "Duplicate post");
            if (!page.hasNext()) break;
            var cursor = page.nextCursor();
            page = postService.getRandomPublicFeed(viewer.getPhoneNumber(), cursor.randomSeed(),
                    cursor.snapshot(), cursor.postId(), 2);
        }
        assertEquals(expected, seen);
        var refreshed = postService.getRandomPublicFeed(viewer.getPhoneNumber(), null, null, null, 2);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstCursor.randomSeed(), refreshed.nextCursor().randomSeed());
        assertThrows(PostBadRequestException.class, () -> postService.getRandomPublicFeed(
                viewer.getPhoneNumber(), null, null, firstCursor.postId(), 2));
        mockMvc.perform(get("/api/community/feed/random").with(user(viewer.getPhoneNumber())).param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.nextCursor.randomSeed").isNotEmpty())
                .andExpect(jsonPath("$.data.nextCursor.snapshot").isNotEmpty());
    }

    private PostResponse createTextPost(User author, String content, PostVisibility visibility) {
        return postService.createPost(
                new CreatePostRequest(
                        PostType.LIFE_UPDATE,
                        content,
                        visibility,
                        null,
                        List.of()
                ),
                author.getPhoneNumber()
        );
    }

    private void makeFriends(User sender, User receiver) {
        FriendRequestResponse request = friendshipService.sendRequest(
                receiver.getId(),
                sender.getPhoneNumber()
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
