package com.example.genz.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Size(max = 30)
    private String phoneNumber;

    @NotBlank
    @Size(max = 72)
    private String password;
}
