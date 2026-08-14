package com.example.genz.dto.AuthDTO;

public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
}
