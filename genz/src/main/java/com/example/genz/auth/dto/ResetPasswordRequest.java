package com.example.genz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(max = 72, message = "New password must be at most 72 characters")
    private String newPassword;

    @NotBlank(message = "Reset token is required")
    private String resetToken;
}
