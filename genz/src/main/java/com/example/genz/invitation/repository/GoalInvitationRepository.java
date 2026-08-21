package com.example.genz.invitation.repository;

import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalInvitationRepository extends JpaRepository<GoalInvitation, UUID> {
    List<GoalInvitation> findByInviteeIdOrderByCreatedAtDesc(UUID inviteeId);
    boolean existsByGoalIdAndInviteeIdAndStatus(UUID goalId, UUID inviteeId, InvitationStatus status);
}
