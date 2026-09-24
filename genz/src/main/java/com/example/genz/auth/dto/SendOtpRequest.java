package com.example.genz.auth.dto;


import com.example.genz.auth.enums.OtpPurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    @Size(max = 30)
    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Phone number must contain 8 to 15 digits")
    private String phoneNumber;

    /** Optional for registration/password reset compatibility; set LOGIN for passwordless login. */
    private OtpPurpose purpose;

}
