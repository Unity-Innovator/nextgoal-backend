package com.example.genz.service.AuthService;

import com.example.genz.dto.AuthDTO.LoginResponse;
import com.example.genz.dto.AuthDTO.SendOtpRequest;
import com.example.genz.dto.AuthDTO.VerifyOtpRequest;

public interface OtpService {
    void sendOtp(SendOtpRequest request);
    LoginResponse verifyOtp(VerifyOtpRequest request);
}
