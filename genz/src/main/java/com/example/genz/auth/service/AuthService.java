package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RefreshTokenRequest;
import com.example.genz.auth.dto.ResetPasswordRequest;
import com.example.genz.auth.dto.StepUpTokenResponse;
import com.example.genz.auth.dto.VerifyPinRequest;

public interface AuthService {
    LoginResponse login(LoginRequest user);
    void register(RegisterRequest request);
    Boolean checkUser(String phoneNumber);
    StepUpTokenResponse verifyPin(VerifyPinRequest request, String phoneNumber);
    LoginResponse refresh(RefreshTokenRequest request);
    Void resetPassword(ResetPasswordRequest request);
}
