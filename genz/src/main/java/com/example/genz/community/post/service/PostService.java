package com.example.genz.community.post.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.post.dto.CreatePostRequest;
import com.example.genz.community.notification.service.CommunityNotificationService;
import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.service.CommunityContentValidator;
import com.example.genz.community.safety.service.CommunityPrivacyService;
import com.example.genz.community.safety.service.CommunityRateLimiter;
import com.example.genz.community.post.dto.PostCursorPageResponse;
import com.example.genz.community.post.dto.PostCursorResponse;
import com.example.genz.community.post.dto.PostMediaUploadResponse;
import com.example.genz.community.post.dto.PostMediaRequest;
import com.example.genz.community.post.dto.PostResponse;
import com.example.genz.community.post.dto.SharePostRequest;
import com.example.genz.community.post.dto.UpdatePostRequest;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostMedia;
import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.enums.PostType;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.community.post.exception.PostBadRequestException;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.post.mapper.PostMapper;
import com.example.genz.community.chat.service.SupabaseStorageService;
import com.example.genz.community.post.repository.PostMediaRepository;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.repository.GoalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PostService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final long MAX_POST_MEDIA_SIZE_BYTES = 40L * 1024 * 1024;

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final PostRepository postRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostMapper mapper;
    private final PostAccessService postAccessService;
    private final CommunityNotificationService notificationService;
    private final CommunityContentValidator contentValidator;
    private final CommunityPrivacyService privacyService;
    private final CommunityRateLimiter rateLimiter;
    private final SupabaseStorageService storageService;

    public PostMediaUploadResponse uploadMedia(MultipartFile file, String currentPhoneNumber) {
        User author = requireCurrentUser(currentPhoneNumber);
        validateUpload(file);
        String storagePath = postStoragePath(author.getId(), file);
        storageService.uploadPostMedia(file, storagePath);
        return new PostMediaUploadResponse(
                mediaType(file),
                storageService.createSignedUrl(storagePath),
                safeFileName(file),
                file.getContentType(),
                file.getSize()
        );
    }

    @Transactional
    public PostResponse createPost(CreatePostRequest request, String currentPhoneNumber) {
        User author = requireCurrentUser(currentPhoneNumber);
        rateLimiter.check(author.getId(), CommunityAction.CREATE_POST);
        if (request.postType() == PostType.SHARED_POST) {
            throw new PostBadRequestException("Use the share endpoint to create a shared post");
        }

        List<PostMediaRequest> requestedMedia = mediaOrEmpty(request.media());
        String content = normalizeContent(request.content());
        PostVisibility visibility = visibilityOrPublic(request.visibility());
        contentValidator.validatePostContent(content);
        contentValidator.validateMedia(requestedMedia);
        privacyService.requirePostVisibilityAllowed(author, visibility);
        requirePostBody(content, requestedMedia);

        Post post = new Post();
        post.setAuthor(author);
        post.setGoal(resolveOwnedGoal(request.goalId(), author));
        post.setPostType(request.postType());
        post.setContent(content);
        post.setVisibility(visibility);
        post.setStatus(PostStatus.ACTIVE);

        postRepository.saveAndFlush(post);
        post.setMedia(saveMedia(post, requestedMedia));
        return mapper.toResponse(post, author.getId());
    }

    @Transactional
    public PostResponse updatePost(
            UUID postId,
            UpdatePostRequest request,
            String currentPhoneNumber
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        rateLimiter.check(currentUser.getId(), CommunityAction.UPDATE_POST);
        Post post = requireActivePostForUpdate(postId);
        requireOwner(post, currentUser);
        requireUpdateFields(request);

        if (request.content() != null) {
            contentValidator.validatePostContent(request.content());
            post.setContent(normalizeContent(request.content()));
        }
        if (request.visibility() != null) {
            privacyService.requirePostVisibilityAllowed(currentUser, request.visibility());
            requireShareVisibility(post, request.visibility());
            post.setVisibility(request.visibility());
        }
        if (request.media() != null) {
            contentValidator.validateMedia(request.media());
            if (post.getPostType() == PostType.SHARED_POST && !request.media().isEmpty()) {
                throw new PostBadRequestException("Shared posts cannot contain additional media");
            }
            List<String> removedStoragePaths = post.getMedia().stream()
                    .map(PostMedia::getStoragePath)
                    .filter(path -> path != null && !path.isBlank())
                    .toList();
            postMediaRepository.deleteAllByPostId(post.getId());
            post.setMedia(new ArrayList<>());
            post.setMedia(saveMedia(post, request.media()));
            scheduleStorageDeletion(removedStoragePaths);
        }

        if (post.getPostType() != PostType.SHARED_POST) {
            requirePostBody(post.getContent(), post.getMedia());
        }
        postRepository.saveAndFlush(post);
        scheduleStorageDeletion(post.getMedia().stream()
                .map(PostMedia::getStoragePath)
                .filter(path -> path != null && !path.isBlank())
                .toList());
        return mapper.toResponse(post, currentUser.getId());
    }

    @Transactional
    public void deletePost(UUID postId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Post post = requireActivePostForUpdate(postId);
        requireOwner(post, currentUser);

        post.setStatus(PostStatus.DELETED);
        post.setDeletedAt(Instant.now());
        post.setPinned(false);
        postRepository.saveAndFlush(post);

        if (post.getSharedPost() != null) {
            postRepository.findByIdForUpdate(post.getSharedPost().getId()).ifPresent(sourcePost -> {
                sourcePost.setShareCount(Math.max(0, sourcePost.getShareCount() - 1));
                postRepository.save(sourcePost);
            });
        }
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(UUID postId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Post post = requireActivePost(postId);
        if (!postAccessService.canView(post, currentUser.getId())) {
            throw invisiblePost();
        }
        return mapper.toResponse(post, currentUser.getId());
    }

    @Transactional(readOnly = true)
    public PostCursorPageResponse getPublicFeed(
            String currentPhoneNumber,
            Instant cursorCreatedAt,
            UUID cursorPostId,
            int limit
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        validateCursor(cursorCreatedAt, cursorPostId);
        List<Post> posts = postRepository.findPublicFeed(
                currentUser.getId(),
                cursorCreatedAt,
                cursorPostId,
                cursorPageRequest(limit)
        );
        return toCursorPage(posts, currentUser.getId(), limit);
    }

    // A seeded hash gives every eligible post a stable random position. The
    // snapshot excludes new posts until refresh; keyset paging survives deletes.
    @Transactional(readOnly = true)
    public PostCursorPageResponse getRandomPublicFeed(
            String currentPhoneNumber, UUID seed, Instant snapshot,
            UUID cursorPostId, int limit
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        if (cursorPostId != null && (seed == null || snapshot == null)) {
            throw new PostBadRequestException("Random feed cursor requires seed and snapshot");
        }
        UUID sessionSeed = seed == null ? UUID.randomUUID() : seed;
        Instant sessionSnapshot = snapshot == null ? Instant.now() : snapshot;
        if (sessionSnapshot.isAfter(Instant.now())) {
            throw new PostBadRequestException("Feed snapshot cannot be in the future");
        }
        String rank = cursorPostId == null ? "" : org.springframework.util.DigestUtils.md5DigestAsHex(
                (cursorPostId.toString() + sessionSeed).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        List<Post> posts = postRepository.findRandomPublicFeed(
                currentUser.getId(), sessionSnapshot, sessionSeed.toString(), rank,
                cursorPostId == null ? new UUID(0, 0) : cursorPostId, cursorPageRequest(limit));
        PostCursorPageResponse page = toCursorPage(posts, currentUser.getId(), limit);
        PostCursorResponse cursor = page.nextCursor();
        return new PostCursorPageResponse(page.items(), cursor == null ? null :
                new PostCursorResponse(cursor.createdAt(), cursor.postId(), sessionSeed, sessionSnapshot),
                page.hasNext());
    }

    @Transactional(readOnly = true)
    public PostCursorPageResponse getFriendsFeed(
            String currentPhoneNumber,
            Instant cursorCreatedAt,
            UUID cursorPostId,
            int limit
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        validateCursor(cursorCreatedAt, cursorPostId);
        List<Post> posts = postRepository.findFriendsFeed(
                currentUser.getId(),
                cursorCreatedAt,
                cursorPostId,
                cursorPageRequest(limit)
        );
        return toCursorPage(posts, currentUser.getId(), limit);
    }

    @Transactional(readOnly = true)
    public PostCursorPageResponse getUserPosts(
            UUID authorId,
            String currentPhoneNumber,
            Instant cursorCreatedAt,
            UUID cursorPostId,
            int limit
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        validateCursor(cursorCreatedAt, cursorPostId);
        if (!userRepository.existsById(authorId)) {
            throw new PostNotFoundException("Post author was not found");
        }
        List<Post> posts = postRepository.findProfileFeed(
                authorId,
                currentUser.getId(),
                cursorCreatedAt,
                cursorPostId,
                cursorPageRequest(limit)
        );
        return toCursorPage(posts, currentUser.getId(), limit);
    }

    @Transactional
    public PostResponse sharePost(
            UUID sourcePostId,
            SharePostRequest request,
            String currentPhoneNumber
    ) {
        User author = requireCurrentUser(currentPhoneNumber);
        rateLimiter.check(author.getId(), CommunityAction.SHARE_POST);
        Post sourcePost = requireActivePostForUpdate(sourcePostId);
        if (!postAccessService.canView(sourcePost, author.getId())) {
            throw invisiblePost();
        }
        if (sourcePost.getVisibility() == PostVisibility.PRIVATE) {
            throw new PostBadRequestException("Private posts cannot be shared");
        }

        PostVisibility shareVisibility = request.visibility() == null
                ? sourcePost.getVisibility()
                : request.visibility();
        contentValidator.validatePostContent(request.content());
        privacyService.requirePostVisibilityAllowed(author, shareVisibility);
        if (sourcePost.getVisibility() == PostVisibility.FRIENDS_ONLY
                && shareVisibility == PostVisibility.PUBLIC) {
            throw new PostBadRequestException("A friends-only post cannot be shared publicly");
        }

        Post share = new Post();
        share.setAuthor(author);
        share.setSharedPost(sourcePost);
        share.setPostType(PostType.SHARED_POST);
        share.setContent(normalizeContent(request.content()));
        share.setVisibility(shareVisibility);
        share.setStatus(PostStatus.ACTIVE);

        postRepository.saveAndFlush(share);
        sourcePost.setShareCount(sourcePost.getShareCount() + 1);
        postRepository.saveAndFlush(sourcePost);
        notificationService.notifyPostShared(
                sourcePost.getAuthor(),
                author,
                sourcePost.getId(),
                share.getId()
        );
        return mapper.toResponse(share, author.getId());
    }

    private Post requireActivePost(UUID postId) {
        return postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(this::invisiblePost);
    }

    private Post requireActivePostForUpdate(UUID postId) {
        return postRepository.findByIdAndStatusForUpdate(postId, PostStatus.ACTIVE)
                .orElseThrow(this::invisiblePost);
    }

    private User requireCurrentUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new PostNotFoundException("Authenticated user was not found"));
    }

    private Goal resolveOwnedGoal(UUID goalId, User author) {
        if (goalId == null) {
            return null;
        }
        return goalRepository.findByIdAndOwner(goalId, author)
                .orElseThrow(() -> new PostNotFoundException("Goal was not found or is not owned by you"));
    }

    private void requireOwner(Post post, User currentUser) {
        if (!post.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the post owner can modify this post");
        }
    }

    private List<PostMedia> saveMedia(Post post, List<PostMediaRequest> requestedMedia) {
        if (requestedMedia.isEmpty()) {
            return new ArrayList<>();
        }
        List<PostMedia> media = new ArrayList<>(requestedMedia.size());
        for (int index = 0; index < requestedMedia.size(); index++) {
            PostMediaRequest item = requestedMedia.get(index);
            PostMedia entity = new PostMedia();
            entity.setPost(post);
            entity.setMediaType(item.mediaType());
            entity.setMediaUrl(item.mediaUrl().trim());
            entity.setStoragePath(storageService.ownedPostStoragePath(
                    item.mediaUrl(),
                    post.getAuthor().getId()
            ));
            entity.setThumbnailUrl(normalizeOptionalText(item.thumbnailUrl()));
            entity.setWidthPx(item.widthPx());
            entity.setHeightPx(item.heightPx());
            entity.setDurationMs(item.durationMs());
            entity.setSortOrder((short) index);
            media.add(entity);
        }
        return postMediaRepository.saveAllAndFlush(media);
    }

    private void requireShareVisibility(Post post, PostVisibility requestedVisibility) {
        if (post.getPostType() == PostType.SHARED_POST
                && post.getSharedPost() != null
                && post.getSharedPost().getVisibility() == PostVisibility.FRIENDS_ONLY
                && requestedVisibility == PostVisibility.PUBLIC) {
            throw new PostBadRequestException("A friends-only post cannot be shared publicly");
        }
    }

    private void requireUpdateFields(UpdatePostRequest request) {
        if (request.content() == null && request.visibility() == null && request.media() == null) {
            throw new PostBadRequestException("At least one field must be provided for update");
        }
    }

    private void requirePostBody(String content, List<?> media) {
        if (content == null && media.isEmpty()) {
            throw new PostBadRequestException("A post must contain text or media");
        }
    }

    private List<PostMediaRequest> mediaOrEmpty(List<PostMediaRequest> media) {
        return media == null ? List.of() : media;
    }

    private String normalizeContent(String content) {
        return normalizeOptionalText(content);
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private PostVisibility visibilityOrPublic(PostVisibility visibility) {
        return visibility == null ? PostVisibility.PUBLIC : visibility;
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new PostBadRequestException("A post media file is required");
        }
        if (file.getSize() > MAX_POST_MEDIA_SIZE_BYTES) {
            throw new PostBadRequestException("Post media must not exceed 40 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new PostBadRequestException("Post media must be an image or video");
        }
        String normalized = contentType.toLowerCase(java.util.Locale.ROOT);
        if ((!normalized.startsWith("image/") || normalized.equals("image/svg+xml"))
                && !normalized.startsWith("video/")) {
            throw new PostBadRequestException("Post media must be an image or video");
        }
    }

    private com.example.genz.community.post.enums.PostMediaType mediaType(MultipartFile file) {
        return file.getContentType().toLowerCase(java.util.Locale.ROOT).startsWith("video/")
                ? com.example.genz.community.post.enums.PostMediaType.VIDEO
                : com.example.genz.community.post.enums.PostMediaType.IMAGE;
    }

    private String postStoragePath(UUID authorId, MultipartFile file) {
        String fileName = safeFileName(file);
        int extensionStart = fileName.lastIndexOf('.');
        String extension = extensionStart > 0 && extensionStart < fileName.length() - 1
                ? fileName.substring(extensionStart).replaceAll("[^A-Za-z0-9.]", "")
                : "";
        if (extension.length() > 11) {
            extension = "";
        }
        return "posts/" + authorId + "/" + UUID.randomUUID() + extension;
    }

    private String safeFileName(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            return "post-media";
        }
        String leafName = fileName.replace('\\', '/');
        leafName = leafName.substring(leafName.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_")
                .trim();
        return leafName.isBlank() ? "post-media" : leafName.substring(0, Math.min(255, leafName.length()));
    }

    private void scheduleStorageDeletion(List<String> storagePaths) {
        if (storagePaths.isEmpty()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storagePaths.forEach(storageService::deleteQuietly);
            }
        });
    }

    private PostCursorPageResponse toCursorPage(List<Post> posts, UUID currentUserId, int limit) {
        boolean hasNext = posts.size() > limit;
        List<Post> pagePosts = hasNext ? posts.subList(0, limit) : posts;
        List<PostResponse> items = pagePosts.stream()
                .map(post -> mapper.toResponse(post, currentUserId))
                .toList();
        PostCursorResponse nextCursor = null;
        if (hasNext && !pagePosts.isEmpty()) {
            Post lastPost = pagePosts.getLast();
            nextCursor = new PostCursorResponse(lastPost.getCreatedAt(), lastPost.getId());
        }
        return new PostCursorPageResponse(items, nextCursor, hasNext);
    }

    private void validateCursor(Instant cursorCreatedAt, UUID cursorPostId) {
        if ((cursorCreatedAt == null) != (cursorPostId == null)) {
            throw new PostBadRequestException(
                    "cursorCreatedAt and cursorPostId must be provided together"
            );
        }
    }

    private PageRequest cursorPageRequest(int limit) {
        if (limit < 1 || limit > MAX_PAGE_SIZE) {
            throw new PostBadRequestException("limit must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(0, limit + 1);
    }

    private PostNotFoundException invisiblePost() {
        return new PostNotFoundException("Post was not found or is not visible to you");
    }

}
