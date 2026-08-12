package com.example.genz.DTO.InvitationDTO;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvitationRequest {

    @NotNull(message = "Invitee ID is required")
    private UUID inviteeId;
}
