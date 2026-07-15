package com.example.genz.auth.dto;

import com.example.genz.auth.enums.Gender;
import lombok.Builder;

import java.time.LocalDate;
import java.util.UUID;


@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        Gender gender,
        LocalDate dateOfBirth,
        String status
) {}
