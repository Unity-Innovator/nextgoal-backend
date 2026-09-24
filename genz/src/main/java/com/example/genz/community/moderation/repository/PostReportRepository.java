package com.example.genz.community.moderation.repository;

import com.example.genz.community.post.entity.PostReport;
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

public interface PostReportRepository extends JpaRepository<PostReport, UUID> {

    boolean existsByPost_IdAndReportedBy_Id(UUID postId, UUID reporterId);

    @EntityGraph(attributePaths = {"post", "post.author", "reportedBy", "reviewedBy"})
    Page<PostReport> findByStatusOrderByCreatedAtAsc(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"post", "post.author", "reportedBy", "reviewedBy"})
    Page<PostReport> findAllByOrderByCreatedAtAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select report from PostReport report
            join fetch report.post post
            join fetch post.author
            join fetch report.reportedBy
            left join fetch report.reviewedBy
            where report.id = :reportId
            """)
    Optional<PostReport> findByIdForUpdate(@Param("reportId") UUID reportId);
}
