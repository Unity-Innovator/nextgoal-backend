package com.example.genz.auth.dto;

import lombok.Builder;

@Builder
public record StepUpTokenResponse(
        String stepUpToken,
        Long expiresIn,
        String tokenType
) {}
