package com.example.genz.auth.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.example.genz.auth.enums.Gender;

import lombok.Builder;


@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        String email,
        Gender gender,
        LocalDate dateOfBirth,
        String status
) {}
