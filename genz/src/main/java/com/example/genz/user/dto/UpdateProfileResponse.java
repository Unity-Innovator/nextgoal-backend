package com.example.genz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileResponse {
    private UUID id;
    private String fullName;
    private String username;
    private String phoneNumber;
    private String avatarUrl;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
}
