package com.example.genz.auth.dto;

import lombok.Builder;

import java.util.UUID;

@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber
) {}
