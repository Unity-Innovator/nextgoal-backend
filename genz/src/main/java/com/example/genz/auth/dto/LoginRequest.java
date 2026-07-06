package com.example.genz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "The Phone Number id required")
    private String phoneNumber;

    @NotBlank
    private String password;
}
