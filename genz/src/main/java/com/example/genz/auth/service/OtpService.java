package com.example.genz.auth.service;

import com.example.genz.auth.dto.ResetTokenResponse;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;

public interface OtpService {
    void sendOtp(SendOtpRequest request);
    void sendRegistrationOtp(String phoneNumber);
    void sendPasswordResetOtp(String phoneNumber);
    void verifyOtp(VerifyOtpRequest request);
    ResetTokenResponse verifyResetOtp(VerifyOtpRequest request);
}
