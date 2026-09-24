package com.example.genz.invitation.repository;

import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface GoalInvitationRepository extends JpaRepository<GoalInvitation, UUID> {
    List<GoalInvitation> findByInviteeIdOrderByCreatedAtDesc(UUID inviteeId);
    boolean existsByGoalIdAndInviteeIdAndStatus(UUID goalId, UUID inviteeId, InvitationStatus status);

    @Query("SELECT COUNT(gi) FROM GoalInvitation gi WHERE gi.inviter.id = :inviterId AND gi.createdAt >= :start AND gi.createdAt <= :end")
    long countByInviterIdAndCreatedAtBetween(
            @Param("inviterId") UUID inviterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT COUNT(gi) FROM GoalInvitation gi WHERE gi.inviter.id = :inviterId AND gi.status = :status AND gi.respondedAt >= :start AND gi.respondedAt <= :end")
    long countByInviterIdAndStatusAndRespondedAtBetween(
            @Param("inviterId") UUID inviterId,
            @Param("status") InvitationStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
