package com.example.genz.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    @Size(max = 30)
    private String phoneNumber;

    @Email
    @Size(max = 255)
    private String email;
}
