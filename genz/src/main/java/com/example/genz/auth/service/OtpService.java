package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;

public interface OtpService {
    void sendOtp(SendOtpRequest request);
    LoginResponse verifyOtp(VerifyOtpRequest request);
}
