package com.example.genz.dto.AuthDTO;

import java.time.LocalDate;
import java.util.UUID;

import com.example.genz.enums.Gender;

import lombok.Builder;


@Builder
public record UserResponse(
        UUID id,
        String fullName,
        String phoneNumber,
        Gender gender,
        LocalDate dateOfBirth,
        String status
) {}
