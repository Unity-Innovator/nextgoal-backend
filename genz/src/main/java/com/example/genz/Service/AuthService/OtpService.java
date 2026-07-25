package com.example.genz.Service.AuthService;

import com.example.genz.DTO.AuthDTO.LoginResponse;
import com.example.genz.DTO.AuthDTO.SendOtpRequest;
import com.example.genz.DTO.AuthDTO.VerifyOtpRequest;

public interface OtpService {
    void sendOtp(SendOtpRequest request);
    LoginResponse verifyOtp(VerifyOtpRequest request);
}
