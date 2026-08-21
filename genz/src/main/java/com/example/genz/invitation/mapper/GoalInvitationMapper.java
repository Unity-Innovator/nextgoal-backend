package com.example.genz.invitation.mapper;

import com.example.genz.invitation.dto.InvitationResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class GoalInvitationMapper {

    public GoalInvitation toEntity(Goal goal, User inviter, User invitee) {
        if (goal == null || inviter == null || invitee == null) {
            return null;
        }
        GoalInvitation invitation = new GoalInvitation();
        invitation.setGoal(goal);
        invitation.setInviter(inviter);
        invitation.setInvitee(invitee);
        invitation.setStatus(InvitationStatus.PENDING);
        return invitation;
    }

    public InvitationResponse toInvitationResponse(GoalInvitation invitation) {
        if (invitation == null) {
            return null;
        }
        return InvitationResponse.builder()
                .invitationId(invitation.getId())
                .goalId(invitation.getGoal() != null ? invitation.getGoal().getId() : null)
                .goalName(invitation.getGoal() != null ? invitation.getGoal().getGoalName() : null)
                .inviterName(invitation.getInviter() != null ? invitation.getInviter().getFullName() : null)
                .status(invitation.getStatus() != null ? invitation.getStatus().name() : null)
                .createdAt(invitation.getCreatedAt())
                .build();
    }

    public List<InvitationResponse> toInvitationResponseList(List<GoalInvitation> invitations) {
        if (invitations == null) {
            return Collections.emptyList();
        }
        return invitations.stream()
                .map(this::toInvitationResponse)
                .collect(Collectors.toList());
    }
}
