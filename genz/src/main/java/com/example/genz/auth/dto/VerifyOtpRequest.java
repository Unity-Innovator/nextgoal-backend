package com.example.genz.auth.dto;


import com.example.genz.auth.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank

    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must contain 8 to 15 digits")
    private String phoneNumber;

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;
}
