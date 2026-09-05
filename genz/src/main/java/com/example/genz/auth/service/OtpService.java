package com.example.genz.auth.service;

import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;

public interface OtpService {
    void sendRegistrationOtp(String phoneNumber);
    RegisterResponse verifyRegistrationOtp(VerifyOtpRequest request);
    void sendPasswordResetOtp(String phoneNumber);
    void resetPassword(ResetPasswordRequest request);
}
