package com.example.genz.Repository.InvitationRepository;

import com.example.genz.Entity.Invitation.GoalInvitation;
import com.example.genz.Enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GoalInvitationRepository extends JpaRepository<GoalInvitation, UUID> {
    List<GoalInvitation> findByInviteeIdOrderByCreatedAtDesc(UUID inviteeId);
    boolean existsByGoalIdAndInviteeIdAndStatus(UUID goalId, UUID inviteeId, InvitationStatus status);
}
