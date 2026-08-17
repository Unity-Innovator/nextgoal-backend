package com.example.genz.Service.InvitationService;

import com.example.genz.DTO.InvitationDTO.CreateInvitationRequest;
import com.example.genz.DTO.InvitationDTO.InvitationResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Entity.Invitation.GoalInvitation;
import com.example.genz.Enums.InvitationStatus;
import com.example.genz.Enums.MemberRole;
import com.example.genz.Enums.MemberStatus;
import com.example.genz.Enums.NotificationType;
import com.example.genz.mapper.GoalInvitationMapper;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalMemberRepository;
import com.example.genz.Repository.GoalRepository.GoalRepository;
import com.example.genz.Repository.InvitationRepository.GoalInvitationRepository;
import com.example.genz.Service.NotificationService.NotificationService;
import com.example.genz.exception.BadRequestException;
import com.example.genz.exception.ResourceNoteFoundException;
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

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Invitation is not in PENDING status");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        GoalInvitation updatedInvitation = goalInvitationRepository.save(invitation);

        Goal goal = invitation.getGoal();
        if (!goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), invitee.getId())) {
            GoalMember member = new GoalMember();
            member.setGoal(goal);
            member.setUser(invitee);
            member.setRole(MemberRole.MEMBER);
            member.setStatus(MemberStatus.ACTIVE);
            goalMemberRepository.save(member);
        }

        notificationService.markNotificationAsReadByReference(invitation.getId(), invitee.getId());

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
