package com.example.genz.invitation.service;

import com.example.genz.invitation.dto.CreateInvitationRequest;
import com.example.genz.invitation.dto.InvitationResponse;

import java.util.List;
import java.util.UUID;

public interface GoalInvitationService {
    InvitationResponse inviteUser(UUID goalId, CreateInvitationRequest request, String inviterPhoneNumber);
    List<InvitationResponse> getReceivedInvitations(String inviteePhoneNumber);
    InvitationResponse acceptInvitation(UUID invitationId, String inviteePhoneNumber);
    InvitationResponse rejectInvitation(UUID invitationId, String inviteePhoneNumber);
}
