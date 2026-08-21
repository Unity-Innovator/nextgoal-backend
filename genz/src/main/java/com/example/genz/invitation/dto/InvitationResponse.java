package com.example.genz.invitation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {
    private UUID invitationId;
    private UUID goalId;
    private String goalName;
    private String inviterName;
    private String status;
    private LocalDateTime createdAt;
}
