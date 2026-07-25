package com.example.genz.DTO.AuthDTO;

public record RegisterResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        UserResponse user
) {
}
