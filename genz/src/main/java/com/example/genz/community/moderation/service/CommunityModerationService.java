package com.example.genz.community.moderation.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.community.moderation.dto.CreateReportRequest;
import com.example.genz.community.moderation.dto.ModerationPageResponse;
import com.example.genz.community.moderation.dto.ModerationReportResponse;
import com.example.genz.community.moderation.dto.ModerationUserResponse;
import com.example.genz.community.moderation.dto.ReviewReportRequest;
import com.example.genz.community.moderation.enums.ReportTargetType;
import com.example.genz.community.moderation.exception.ModerationBadRequestException;
import com.example.genz.community.moderation.exception.ModerationConflictException;
import com.example.genz.community.moderation.exception.ModerationNotFoundException;
import com.example.genz.community.moderation.repository.CommentReportRepository;
import com.example.genz.community.moderation.repository.PostReportRepository;
import com.example.genz.community.post.entity.CommentReport;
import com.example.genz.community.post.entity.Post;
import com.example.genz.community.post.entity.PostComment;
import com.example.genz.community.post.entity.PostReport;
import com.example.genz.community.post.enums.CommentStatus;
import com.example.genz.community.post.enums.PostStatus;
import com.example.genz.community.post.enums.ReportStatus;
import com.example.genz.community.post.repository.PostCommentRepository;
import com.example.genz.community.post.repository.PostRepository;
import com.example.genz.community.post.service.PostAccessService;
import com.example.genz.community.safety.enums.CommunityAction;
import com.example.genz.community.safety.service.CommunityContentValidator;
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
public class CommunityModerationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostCommentRepository commentRepository;
    private final PostReportRepository postReportRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostAccessService postAccessService;
    private final CommunityContentValidator contentValidator;
    private final CommunityRateLimiter rateLimiter;

    @Transactional
    public ModerationReportResponse reportPost(
            UUID postId,
            CreateReportRequest request,
            String currentPhoneNumber
    ) {
        User reporter = requireCurrentUser(currentPhoneNumber);
        Post post = postRepository.findByIdAndStatus(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new ModerationNotFoundException("Post was not found or is not visible to you"));
        if (!postAccessService.canView(post, reporter.getId())) {
            throw new ModerationNotFoundException("Post was not found or is not visible to you");
        }
        requireNotOwner(post.getAuthor().getId(), reporter.getId());
        requireNoPostReport(postId, reporter.getId());
        contentValidator.validateReportDetails(request.details());
        rateLimiter.check(reporter.getId(), CommunityAction.REPORT_CONTENT);

        PostReport report = new PostReport();
        report.setPost(post);
        report.setReportedBy(reporter);
        report.setReason(request.reason());
        report.setDetails(normalizeOptional(request.details()));
        report.setStatus(ReportStatus.PENDING);
        try {
            return toResponse(postReportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException exception) {
            throw new ModerationConflictException("You have already reported this post");
        }
    }

    @Transactional
    public ModerationReportResponse reportComment(
            UUID commentId,
            CreateReportRequest request,
            String currentPhoneNumber
    ) {
        User reporter = requireCurrentUser(currentPhoneNumber);
        PostComment comment = commentRepository.findDetailedById(commentId)
                .filter(item -> item.getStatus() == CommentStatus.ACTIVE)
                .filter(item -> item.getPost().getStatus() == PostStatus.ACTIVE)
                .orElseThrow(() -> new ModerationNotFoundException("Comment was not found or is not visible to you"));
        if (!postAccessService.canView(comment.getPost(), reporter.getId())) {
            throw new ModerationNotFoundException("Comment was not found or is not visible to you");
        }
        requireNotOwner(comment.getAuthor().getId(), reporter.getId());
        requireNoCommentReport(commentId, reporter.getId());
        contentValidator.validateReportDetails(request.details());
        rateLimiter.check(reporter.getId(), CommunityAction.REPORT_CONTENT);

        CommentReport report = new CommentReport();
        report.setComment(comment);
        report.setReportedBy(reporter);
        report.setReason(request.reason());
        report.setDetails(normalizeOptional(request.details()));
        report.setStatus(ReportStatus.PENDING);
        try {
            return toResponse(commentReportRepository.saveAndFlush(report));
        } catch (DataIntegrityViolationException exception) {
            throw new ModerationConflictException("You have already reported this comment");
        }
    }

    @Transactional(readOnly = true)
    public ModerationPageResponse<ModerationReportResponse> getPostReports(
            ReportStatus status,
            int page,
            int size,
            String currentPhoneNumber
    ) {
        requireAdmin(currentPhoneNumber);
        Page<PostReport> reports = status == null
                ? postReportRepository.findAllByOrderByCreatedAtAsc(pageRequest(page, size))
                : postReportRepository.findByStatusOrderByCreatedAtAsc(status, pageRequest(page, size));
        return ModerationPageResponse.from(reports.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ModerationPageResponse<ModerationReportResponse> getCommentReports(
            ReportStatus status,
            int page,
            int size,
            String currentPhoneNumber
    ) {
        requireAdmin(currentPhoneNumber);
        Page<CommentReport> reports = status == null
                ? commentReportRepository.findAllByOrderByCreatedAtAsc(pageRequest(page, size))
                : commentReportRepository.findByStatusOrderByCreatedAtAsc(status, pageRequest(page, size));
        return ModerationPageResponse.from(reports.map(this::toResponse));
    }

    @Transactional
    public ModerationReportResponse reviewPostReport(
            UUID reportId,
            ReviewReportRequest request,
            String currentPhoneNumber
    ) {
        User admin = requireAdmin(currentPhoneNumber);
        validateReviewStatus(request.status());
        contentValidator.validateReportDetails(request.moderationNote());
        PostReport report = postReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ModerationNotFoundException("Post report not found"));
        requirePending(report.getStatus());
        if (request.status() == ReportStatus.ACTION_TAKEN) {
            removePostInternal(report.getPost().getId());
        }
        report.setStatus(request.status());
        report.setReviewedBy(admin);
        report.setReviewedAt(Instant.now());
        report.setModerationNote(normalizeOptional(request.moderationNote()));
        return toResponse(postReportRepository.saveAndFlush(report));
    }

    @Transactional
    public ModerationReportResponse reviewCommentReport(
            UUID reportId,
            ReviewReportRequest request,
            String currentPhoneNumber
    ) {
        User admin = requireAdmin(currentPhoneNumber);
        validateReviewStatus(request.status());
        contentValidator.validateReportDetails(request.moderationNote());
        CommentReport report = commentReportRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ModerationNotFoundException("Comment report not found"));
        requirePending(report.getStatus());
        if (request.status() == ReportStatus.ACTION_TAKEN) {
            removeCommentInternal(report.getComment().getId());
        }
        report.setStatus(request.status());
        report.setReviewedBy(admin);
        report.setReviewedAt(Instant.now());
        report.setModerationNote(normalizeOptional(request.moderationNote()));
        return toResponse(commentReportRepository.saveAndFlush(report));
    }

    @Transactional
    public void removePost(UUID postId, String currentPhoneNumber) {
        requireAdmin(currentPhoneNumber);
        removePostInternal(postId);
    }

    @Transactional
    public void removeComment(UUID commentId, String currentPhoneNumber) {
        requireAdmin(currentPhoneNumber);
        removeCommentInternal(commentId);
    }

    private void removePostInternal(UUID postId) {
        Post post = postRepository.findByIdForUpdate(postId)
                .orElseThrow(() -> new ModerationNotFoundException("Post not found"));
        if (post.getStatus() == PostStatus.HIDDEN || post.getStatus() == PostStatus.DELETED) {
            return;
        }
        post.setStatus(PostStatus.HIDDEN);
        post.setDeletedAt(Instant.now());
        post.setPinned(false);
        postRepository.save(post);
        if (post.getSharedPost() != null) {
            postRepository.findByIdForUpdate(post.getSharedPost().getId()).ifPresent(source -> {
                source.setShareCount(Math.max(0, source.getShareCount() - 1));
                postRepository.save(source);
            });
        }
        postRepository.flush();
    }

    private void removeCommentInternal(UUID commentId) {
        PostComment preview = commentRepository.findDetailedById(commentId)
                .orElseThrow(() -> new ModerationNotFoundException("Comment not found"));
        Post post = postRepository.findByIdForUpdate(preview.getPost().getId())
                .orElseThrow(() -> new ModerationNotFoundException("Post not found"));
        PostComment comment = commentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> new ModerationNotFoundException("Comment not found"));
        if (comment.getStatus() != CommentStatus.ACTIVE) {
            return;
        }
        comment.setStatus(CommentStatus.HIDDEN);
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
        post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
        if (comment.getParentComment() != null) {
            commentRepository.findByIdForUpdate(comment.getParentComment().getId()).ifPresent(parent -> {
                parent.setReplyCount(Math.max(0, parent.getReplyCount() - 1));
                commentRepository.save(parent);
            });
        }
        postRepository.saveAndFlush(post);
    }

    private ModerationReportResponse toResponse(PostReport report) {
        return new ModerationReportResponse(
                report.getId(),
                ReportTargetType.POST,
                report.getPost().getId(),
                report.getPost().getAuthor().getId(),
                report.getPost().getContent(),
                toUser(report.getReportedBy()),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                toUser(report.getReviewedBy()),
                report.getModerationNote(),
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }

    private ModerationReportResponse toResponse(CommentReport report) {
        return new ModerationReportResponse(
                report.getId(),
                ReportTargetType.COMMENT,
                report.getComment().getId(),
                report.getComment().getAuthor().getId(),
                report.getComment().getContent(),
                toUser(report.getReportedBy()),
                report.getReason(),
                report.getDetails(),
                report.getStatus(),
                toUser(report.getReviewedBy()),
                report.getModerationNote(),
                report.getReviewedAt(),
                report.getCreatedAt()
        );
    }

    private ModerationUserResponse toUser(User user) {
        return user == null ? null : new ModerationUserResponse(user.getId(), user.getFullName());
    }

    private User requireCurrentUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ModerationNotFoundException("Authenticated user was not found"));
    }

    private User requireAdmin(String phoneNumber) {
        User user = requireCurrentUser(phoneNumber);
        if (user.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Administrator access is required");
        }
        return user;
    }

    private void requireNotOwner(UUID ownerId, UUID reporterId) {
        if (ownerId.equals(reporterId)) {
            throw new ModerationBadRequestException("You cannot report your own content");
        }
    }

    private void requireNoPostReport(UUID postId, UUID reporterId) {
        if (postReportRepository.existsByPost_IdAndReportedBy_Id(postId, reporterId)) {
            throw new ModerationConflictException("You have already reported this post");
        }
    }

    private void requireNoCommentReport(UUID commentId, UUID reporterId) {
        if (commentReportRepository.existsByComment_IdAndReportedBy_Id(commentId, reporterId)) {
            throw new ModerationConflictException("You have already reported this comment");
        }
    }

    private void validateReviewStatus(ReportStatus status) {
        if (status == ReportStatus.PENDING) {
            throw new ModerationBadRequestException("A reviewed report cannot remain pending");
        }
    }

    private void requirePending(ReportStatus status) {
        if (status != ReportStatus.PENDING) {
            throw new ModerationConflictException("This report has already been reviewed");
        }
    }

    private PageRequest pageRequest(int page, int size) {
        if (page < 0) {
            throw new ModerationBadRequestException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ModerationBadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
