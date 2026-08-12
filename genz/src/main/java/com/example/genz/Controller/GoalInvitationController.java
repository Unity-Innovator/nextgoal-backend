package com.example.genz.Controller;

import com.example.genz.DTO.InvitationDTO.CreateInvitationRequest;
import com.example.genz.DTO.InvitationDTO.InvitationResponse;
import com.example.genz.Service.InvitationService.GoalInvitationService;
import com.example.genz.exception.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GoalInvitationController {

    private final GoalInvitationService goalInvitationService;

    @PostMapping("/api/goals/{goalId}/invitations")
    public ResponseEntity<ApiResponse<InvitationResponse>> inviteUser(
            @PathVariable UUID goalId,
            @Valid @RequestBody CreateInvitationRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        InvitationResponse response = goalInvitationService.inviteUser(goalId, request, phoneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User invited successfully", response));
    }

    @GetMapping("/api/invitations")
    public ResponseEntity<ApiResponse<List<InvitationResponse>>> getMyReceivedInvitations(
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        List<InvitationResponse> response = goalInvitationService.getReceivedInvitations(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Received invitations retrieved successfully", response));
    }

    @PutMapping("/api/invitations/{invitationId}/accept")
    public ResponseEntity<ApiResponse<InvitationResponse>> acceptInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        InvitationResponse response = goalInvitationService.acceptInvitation(invitationId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Invitation accepted successfully", response));
    }

    @PutMapping("/api/invitations/{invitationId}/reject")
    public ResponseEntity<ApiResponse<InvitationResponse>> rejectInvitation(
            @PathVariable UUID invitationId,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        InvitationResponse response = goalInvitationService.rejectInvitation(invitationId, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Invitation rejected successfully", response));
    }
}
