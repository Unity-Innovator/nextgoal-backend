package com.example.genz.Service.InvitationService;

import com.example.genz.DTO.InvitationDTO.CreateInvitationRequest;
import com.example.genz.DTO.InvitationDTO.InvitationResponse;

import java.util.List;
import java.util.UUID;

public interface GoalInvitationService {
    InvitationResponse inviteUser(UUID goalId, CreateInvitationRequest request, String inviterPhoneNumber);
    List<InvitationResponse> getReceivedInvitations(String inviteePhoneNumber);
    InvitationResponse acceptInvitation(UUID invitationId, String inviteePhoneNumber);
    InvitationResponse rejectInvitation(UUID invitationId, String inviteePhoneNumber);
}
