package com.example.genz.community.moderation.repository;

import com.example.genz.community.post.entity.CommentReport;
import com.example.genz.community.post.enums.ReportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {

    boolean existsByComment_IdAndReportedBy_Id(UUID commentId, UUID reporterId);

    @EntityGraph(attributePaths = {"comment", "comment.author", "reportedBy", "reviewedBy"})
    Page<CommentReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"comment", "comment.author", "reportedBy", "reviewedBy"})
    Page<CommentReport> findAllByOrderByCreatedAtAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select report from CommentReport report
            join fetch report.comment comment
            join fetch comment.author
            join fetch comment.post
            join fetch report.reportedBy
            left join fetch report.reviewedBy
            where report.id = :reportId
            """)
    Optional<CommentReport> findByIdForUpdate(@Param("reportId") UUID reportId);
}
