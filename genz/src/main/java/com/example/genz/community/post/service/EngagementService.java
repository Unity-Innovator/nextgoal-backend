package com.example.genz.community.post.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.post.dto.CommentRequest;
import com.example.genz.community.notification.service.CommunityNotificationService;
import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.service.CommunityContentValidator;
import com.example.genz.community.safety.service.CommunityPrivacyService;
import com.example.genz.community.safety.service.CommunityRateLimiter;
import com.example.genz.community.post.dto.CommentResponse;
import com.example.genz.community.post.dto.EngagementLikeResponse;
import com.example.genz.community.post.entity.CommentLike;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostComment;
import com.example.genz.community.post.entity.PostLike;
import com.example.genz.community.post.enums.CommentStatus;
import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.exception.PostNotFoundException;
import com.example.genz.community.post.mapper.CommentMapper;
import com.example.genz.community.post.repository.CommentLikeRepository;
import com.example.genz.community.post.repository.PostCommentRepository;
import com.example.genz.community.post.repository.PostLikeRepository;
import com.example.genz.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EngagementService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostCommentRepository postCommentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostAccessService postAccessService;
    private final CommentMapper commentMapper;
    private final CommunityNotificationService notificationService;
    private final CommunityContentValidator contentValidator;
    private final CommunityPrivacyService privacyService;
    private final CommunityRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getComments(UUID postId, String phone, int page, int size) {
        User viewer = requireCurrentUser(phone);
        Post post = postRepository.findById(postId).orElseThrow(this::invisiblePost);
        if (post.getStatus() != PostStatus.ACTIVE || !postAccessService.canView(post, viewer.getId())) {
            throw invisiblePost();
        }
        var rows = postCommentRepository.findByPost_IdAndStatus(postId, CommentStatus.ACTIVE,
                org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)),
                        org.springframework.data.domain.Sort.by("createdAt", "id")));
        var items = rows.getContent().stream().map(comment -> commentMapper.toResponse(comment,
                viewer.getId(), commentLikeRepository.existsByComment_IdAndUser_Id(comment.getId(), viewer.getId())))
                .toList();
        return java.util.Map.of("items", items, "hasNext", rows.hasNext());
    }

    @Transactional
    public EngagementLikeResponse likePost(UUID postId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Post post = requireVisiblePostForUpdate(postId, currentUser);
        rateLimiter.check(currentUser.getId(), CommunityAction.POST_LIKE);
        if (postLikeRepository.findByPost_IdAndUser_Id(postId, currentUser.getId()).isPresent()) {
            return new EngagementLikeResponse(true, false, post.getLikeCount());
        }

        PostLike like = new PostLike();
        like.setPost(post);
        like.setUser(currentUser);
        postLikeRepository.saveAndFlush(like);

        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.saveAndFlush(post);
        notificationService.notifyPostLiked(post.getAuthor(), currentUser, post.getId());
        return new EngagementLikeResponse(true, true, post.getLikeCount());
    }

    @Transactional
    public EngagementLikeResponse unlikePost(UUID postId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Post post = requireVisiblePostForUpdate(postId, currentUser);
        return postLikeRepository.findByPost_IdAndUser_Id(postId, currentUser.getId())
                .map(like -> {
                    postLikeRepository.delete(like);
                    postLikeRepository.flush();
                    post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
                    postRepository.saveAndFlush(post);
                    return new EngagementLikeResponse(false, true, post.getLikeCount());
                })
                .orElseGet(() -> new EngagementLikeResponse(false, false, post.getLikeCount()));
    }

    @Transactional
    public CommentResponse addComment(
            UUID postId,
            CommentRequest request,
            String currentPhoneNumber
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        Post post = requireVisiblePostForUpdate(postId, currentUser);
        if (!privacyService.canComment(post.getAuthor(), currentUser.getId())) {
            throw new AccessDeniedException("The post owner has disabled comments");
        }
        contentValidator.validateCommentContent(request.content());
        rateLimiter.check(currentUser.getId(), CommunityAction.COMMENT_WRITE);

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setAuthor(currentUser);
        comment.setContent(request.content().trim());
        comment.setStatus(CommentStatus.ACTIVE);
        postCommentRepository.saveAndFlush(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postRepository.saveAndFlush(post);
        notificationService.notifyPostCommented(
                post.getAuthor(),
                currentUser,
                post.getId(),
                comment.getId()
        );
        return commentMapper.toResponse(comment, currentUser.getId(), false);
    }

    @Transactional
    public CommentResponse replyToComment(
            UUID parentCommentId,
            CommentRequest request,
            String currentPhoneNumber
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        PostComment preview = requireComment(parentCommentId);
        Post post = requireVisiblePostForUpdate(preview.getPost().getId(), currentUser);
        if (!privacyService.canComment(post.getAuthor(), currentUser.getId())) {
            throw new AccessDeniedException("The post owner has disabled comments");
        }
        contentValidator.validateCommentContent(request.content());
        rateLimiter.check(currentUser.getId(), CommunityAction.COMMENT_WRITE);
        PostComment parent = requireActiveCommentForUpdate(parentCommentId);
        requireSamePost(parent, post);

        PostComment reply = new PostComment();
        reply.setPost(post);
        reply.setAuthor(currentUser);
        reply.setParentComment(parent);
        reply.setContent(request.content().trim());
        reply.setStatus(CommentStatus.ACTIVE);
        postCommentRepository.saveAndFlush(reply);

        parent.setReplyCount(parent.getReplyCount() + 1);
        post.setCommentCount(post.getCommentCount() + 1);
        postCommentRepository.save(parent);
        postRepository.saveAndFlush(post);
        notificationService.notifyCommentReplied(
                parent.getAuthor(),
                currentUser,
                post.getId(),
                parent.getId(),
                reply.getId()
        );
        return commentMapper.toResponse(reply, currentUser.getId(), false);
    }

    @Transactional
    public CommentResponse editComment(
            UUID commentId,
            CommentRequest request,
            String currentPhoneNumber
    ) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        LockedComment locked = requireVisibleCommentForUpdate(commentId, currentUser);
        requireCommentOwner(locked.comment(), currentUser);
        contentValidator.validateCommentContent(request.content());
        rateLimiter.check(currentUser.getId(), CommunityAction.COMMENT_WRITE);

        locked.comment().setContent(request.content().trim());
        postCommentRepository.saveAndFlush(locked.comment());
        boolean liked = commentLikeRepository.existsByComment_IdAndUser_Id(
                commentId,
                currentUser.getId()
        );
        return commentMapper.toResponse(locked.comment(), currentUser.getId(), liked);
    }

    @Transactional
    public void deleteComment(UUID commentId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        LockedComment locked = requireVisibleCommentForUpdate(commentId, currentUser);
        PostComment comment = locked.comment();
        requireCommentOwner(comment, currentUser);

        comment.setStatus(CommentStatus.DELETED);
        comment.setDeletedAt(Instant.now());
        postCommentRepository.save(comment);

        Post post = locked.post();
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));

        if (comment.getParentComment() != null) {
            postCommentRepository.findByIdForUpdate(comment.getParentComment().getId())
                    .ifPresent(parent -> {
                        parent.setReplyCount(Math.max(0, parent.getReplyCount() - 1));
                        postCommentRepository.save(parent);
                    });
        }
        postRepository.saveAndFlush(post);
    }

    @Transactional
    public EngagementLikeResponse likeComment(UUID commentId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        LockedComment locked = requireVisibleCommentForUpdate(commentId, currentUser);
        rateLimiter.check(currentUser.getId(), CommunityAction.COMMENT_LIKE);
        PostComment comment = locked.comment();
        if (commentLikeRepository.findByComment_IdAndUser_Id(commentId, currentUser.getId()).isPresent()) {
            return new EngagementLikeResponse(true, false, comment.getLikeCount());
        }

        CommentLike like = new CommentLike();
        like.setComment(comment);
        like.setUser(currentUser);
        commentLikeRepository.saveAndFlush(like);

        comment.setLikeCount(comment.getLikeCount() + 1);
        postCommentRepository.saveAndFlush(comment);
        return new EngagementLikeResponse(true, true, comment.getLikeCount());
    }

    @Transactional
    public EngagementLikeResponse unlikeComment(UUID commentId, String currentPhoneNumber) {
        User currentUser = requireCurrentUser(currentPhoneNumber);
        LockedComment locked = requireVisibleCommentForUpdate(commentId, currentUser);
        PostComment comment = locked.comment();
        return commentLikeRepository.findByComment_IdAndUser_Id(commentId, currentUser.getId())
                .map(like -> {
                    commentLikeRepository.delete(like);
                    commentLikeRepository.flush();
                    comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
                    postCommentRepository.saveAndFlush(comment);
                    return new EngagementLikeResponse(false, true, comment.getLikeCount());
                })
                .orElseGet(() -> new EngagementLikeResponse(false, false, comment.getLikeCount()));
    }

    private LockedComment requireVisibleCommentForUpdate(UUID commentId, User currentUser) {
        PostComment preview = requireComment(commentId);
        Post post = requireVisiblePostForUpdate(preview.getPost().getId(), currentUser);
        PostComment comment = requireActiveCommentForUpdate(commentId);
        requireSamePost(comment, post);
        return new LockedComment(post, comment);
    }

    private Post requireVisiblePostForUpdate(UUID postId, User currentUser) {
        Post post = postRepository.findByIdAndStatusForUpdate(postId, PostStatus.ACTIVE)
                .orElseThrow(this::invisiblePost);
        if (!postAccessService.canView(post, currentUser.getId())) {
            throw invisiblePost();
        }
        return post;
    }

    private PostComment requireComment(UUID commentId) {
        return postCommentRepository.findDetailedById(commentId)
                .orElseThrow(this::invisibleComment);
    }

    private PostComment requireActiveCommentForUpdate(UUID commentId) {
        PostComment comment = postCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(this::invisibleComment);
        if (comment.getStatus() != CommentStatus.ACTIVE) {
            throw invisibleComment();
        }
        return comment;
    }

    private User requireCurrentUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new PostNotFoundException("Authenticated user was not found"));
    }

    private void requireCommentOwner(PostComment comment, User currentUser) {
        if (!comment.getAuthor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Only the comment owner can modify this comment");
        }
    }

    private void requireSamePost(PostComment comment, Post post) {
        if (!comment.getPost().getId().equals(post.getId())) {
            throw invisibleComment();
        }
    }

    private PostNotFoundException invisiblePost() {
        return new PostNotFoundException("Post was not found or is not visible to you");
    }

    private PostNotFoundException invisibleComment() {
        return new PostNotFoundException("Comment was not found or is not visible to you");
    }

    private record LockedComment(Post post, PostComment comment) {
    }
}
