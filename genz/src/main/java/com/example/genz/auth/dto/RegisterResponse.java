package com.example.genz.auth.dto;

public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
}
