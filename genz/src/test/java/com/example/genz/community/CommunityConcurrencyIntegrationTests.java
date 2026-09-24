package com.example.genz.community;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.moderation.dto.CreateReportRequest;
import com.example.genz.community.moderation.exception.ModerationConflictException;
import com.example.genz.community.moderation.repository.PostReportRepository;
import com.example.genz.community.moderation.service.CommunityModerationService;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.post.dto.EngagementLikeResponse;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.enums.CommentStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.enums.ReportReason;
import com.example.genz.community.post.repository.PostCommentRepository;
import com.example.genz.community.post.repository.PostLikeRepository;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.community.post.service.EngagementService;
import com.example.genz.community.post.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_community_concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
class CommunityConcurrencyIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostCommentRepository commentRepository;

    @Autowired
    private PostReportRepository postReportRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private EngagementService engagementService;

    @Autowired
    private CommunityModerationService moderationService;

    @Test
    void concurrentDuplicateLikesRemainIdempotentAndCounterConsistent() throws Exception {
        User author = saveUser("Like Author", "0170000001");
        User liker = saveUser("Concurrent Liker", "0170000002");
        PostResponse post = createPost(author, "Concurrent like post");

        List<EngagementLikeResponse> responses = runConcurrently(
                8,
                () -> engagementService.likePost(post.id(), liker.getPhoneNumber())
        );

        assertEquals(1, responses.stream().filter(EngagementLikeResponse::changed).count());
        assertEquals(1, postLikeRepository.countByPost_Id(post.id()));
        Post storedPost = postRepository.findById(post.id()).orElseThrow();
        assertEquals(1, storedPost.getLikeCount());
    }

    @Test
    void concurrentCommentsKeepTheDenormalizedCounterConsistent() throws Exception {
        User author = saveUser("Comment Author", "0171000001");
        User commenter = saveUser("Concurrent Commenter", "0171000002");
        PostResponse post = createPost(author, "Concurrent comment post");

        runConcurrently(
                8,
                () -> engagementService.addComment(
                        post.id(),
                        new CommentRequest("Concurrent comment"),
                        commenter.getPhoneNumber()
                )
        );

        long storedComments = commentRepository.countByPost_IdAndStatus(
                post.id(),
                CommentStatus.ACTIVE
        );
        Post storedPost = postRepository.findById(post.id()).orElseThrow();
        assertEquals(8, storedComments);
        assertEquals(storedComments, storedPost.getCommentCount());
    }

    @Test
    void concurrentDuplicateReportsAreStoppedByTheDatabaseConstraint() throws Exception {
        User author = saveUser("Report Author", "0172000001");
        User reporter = saveUser("Concurrent Reporter", "0172000002");
        PostResponse post = createPost(author, "Concurrent report post");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Void>> futures = new ArrayList<>();
            for (int index = 0; index < 2; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    moderationService.reportPost(
                            post.id(),
                            new CreateReportRequest(ReportReason.SPAM, "Concurrent duplicate"),
                            reporter.getPhoneNumber()
                    );
                    return null;
                }));
            }
            ready.await();
            start.countDown();

            int successes = 0;
            int conflicts = 0;
            for (Future<Void> future : futures) {
                try {
                    future.get();
                    successes++;
                } catch (ExecutionException exception) {
                    assertInstanceOf(ModerationConflictException.class, exception.getCause());
                    conflicts++;
                }
            }
            assertEquals(1, successes);
            assertEquals(1, conflicts);
        }
        assertEquals(1, postReportRepository.count());
    }

    private <T> List<T> runConcurrently(int taskCount, ThrowingSupplier<T> task) throws Exception {
        CountDownLatch ready = new CountDownLatch(taskCount);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(taskCount)) {
            List<Future<T>> futures = new ArrayList<>();
            for (int index = 0; index < taskCount; index++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.get();
                }));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        }
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

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
