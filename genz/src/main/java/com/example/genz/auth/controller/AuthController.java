package com.example.genz.auth.controller;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.service.AuthService;
import com.example.genz.auth.service.OtpService;
import com.example.genz.common.exception.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("success", authService.login(request)));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request);
        return ResponseEntity.ok(new ApiResponse<>("OTP sent successfully", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        LoginResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(new ApiResponse<>("OTP verified successfully", response));
    }
}
