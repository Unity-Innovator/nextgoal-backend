package com.example.genz.DTO.AuthDTO;

import com.example.genz.Enums.Gender;
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
