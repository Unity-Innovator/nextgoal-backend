package com.example.genz.DTO.AuthDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendOtpRequest {
    @NotBlank
    private String phoneNumber;
}
