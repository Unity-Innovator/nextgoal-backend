package com.example.genz.invitation.service;

import com.example.genz.invitation.dto.CreateInvitationRequest;
import com.example.genz.invitation.dto.InvitationResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.invitation.entity.GoalInvitation;
import com.example.genz.invitation.enums.InvitationStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.notification.enums.NotificationType;
import com.example.genz.invitation.mapper.GoalInvitationMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.invitation.repository.GoalInvitationRepository;
import com.example.genz.notification.service.NotificationService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalInvitationServiceImpl implements GoalInvitationService {

    private final GoalInvitationRepository goalInvitationRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final GoalInvitationMapper goalInvitationMapper;
    private final com.example.genz.challenge.service.ChallengeCompletionService challengeCompletionService;

    @Override
    @Transactional
    public InvitationResponse inviteUser(UUID goalId, CreateInvitationRequest request, String inviterPhoneNumber) {
        User inviter = userRepository.findByPhoneNumber(inviterPhoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("Inviter user not found"));

        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        // 1. Only goal owner can invite users
        if (!goal.getOwner().getId().equals(inviter.getId())) {
            throw new BadRequestException("Only the goal owner can invite users to this goal");
        }

        // 2. Cannot invite yourself
        if (inviter.getId().equals(request.getInviteeId())) {
            throw new BadRequestException("You cannot invite yourself");
        }

        User invitee = userRepository.findById(request.getInviteeId())
                .orElseThrow(() -> new ResourceNoteFoundException("Invitee user not found"));

        // 3. Cannot invite existing members
        if (goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), invitee.getId())) {
            throw new BadRequestException("User is already a member of this goal");
        }

        // 4. Cannot create duplicate pending invitation
        if (goalInvitationRepository.existsByGoalIdAndInviteeIdAndStatus(goal.getId(), invitee.getId(), InvitationStatus.PENDING)) {
            throw new BadRequestException("Pending invitation already exists for this user");
        }

        GoalInvitation invitation = goalInvitationMapper.toEntity(goal, inviter, invitee);
        GoalInvitation savedInvitation = goalInvitationRepository.save(invitation);

        // 5. Create notification for invitee
        String title = "Goal Invitation";
        String message = inviter.getFullName() + " invited you to join " + goal.getGoalName() + " goal";
        notificationService.createNotification(invitee, title, message, NotificationType.GOAL_INVITATION, savedInvitation.getId());

        try {
            challengeCompletionService.evaluateInvitationChallenges(inviter);
        } catch (Exception ignored) {}

        return goalInvitationMapper.toInvitationResponse(savedInvitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationResponse> getReceivedInvitations(String inviteePhoneNumber) {
        User invitee = userRepository.findByPhoneNumber(inviteePhoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        List<GoalInvitation> invitations = goalInvitationRepository.findByInviteeIdOrderByCreatedAtDesc(invitee.getId());
        return goalInvitationMapper.toInvitationResponseList(invitations);
    }

    @Override
    @Transactional
    public InvitationResponse acceptInvitation(UUID invitationId, String inviteePhoneNumber) {
        User invitee = userRepository.findByPhoneNumber(inviteePhoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        GoalInvitation invitation = goalInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNoteFoundException("Invitation not found"));

        if (!invitation.getInvitee().getId().equals(invitee.getId())) {
            throw new BadRequestException("This invitation does not belong to you");
        }

        if (invitation.getStatus() == InvitationStatus.ACCEPTED) {
            Goal goal = invitation.getGoal();
            GoalMember member = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), invitee.getId())
                    .orElseGet(() -> {
                        GoalMember newMember = new GoalMember();
                        newMember.setGoal(goal);
                        newMember.setUser(invitee);
                        newMember.setRole(MemberRole.MEMBER);
                        return newMember;
                    });
            member.setStatus(MemberStatus.ACTIVE);
            goalMemberRepository.save(member);
            return goalInvitationMapper.toInvitationResponse(invitation);
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is not in PENDING status");
        }

        Goal goal = invitation.getGoal();
        GoalMember member = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), invitee.getId())
                .orElseGet(() -> {
                    GoalMember newMember = new GoalMember();
                    newMember.setGoal(goal);
                    newMember.setUser(invitee);
                    newMember.setRole(MemberRole.MEMBER);
                    return newMember;
                });
        member.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(member);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        GoalInvitation updatedInvitation = goalInvitationRepository.save(invitation);

        notificationService.markNotificationAsReadByReference(invitation.getId(), invitee.getId());

        try {
            challengeCompletionService.evaluateFriendJoinedChallenges(invitation.getInviter());
        } catch (Exception ignored) {}

        return goalInvitationMapper.toInvitationResponse(updatedInvitation);
    }

    @Override
    @Transactional
    public InvitationResponse rejectInvitation(UUID invitationId, String inviteePhoneNumber) {
        User invitee = userRepository.findByPhoneNumber(inviteePhoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        GoalInvitation invitation = goalInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNoteFoundException("Invitation not found"));

        if (!invitation.getInvitee().getId().equals(invitee.getId())) {
            throw new BadRequestException("This invitation does not belong to you");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is not in PENDING status");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        GoalInvitation updatedInvitation = goalInvitationRepository.save(invitation);

        notificationService.markNotificationAsReadByReference(invitation.getId(), invitee.getId());

        return goalInvitationMapper.toInvitationResponse(updatedInvitation);
    }
}
