package com.example.genz.auth.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.RefreshTokenRequest;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.StepUpTokenResponse;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.dto.VerifyPinRequest;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.auth.service.AuthService;
import com.example.genz.auth.service.OtpService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthService authService;

    @Autowired
    private OtpService otpService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success("success", authService.login(request)));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success("success", authService.register(request)));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", null));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        LoginResponse response = otpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
    }

    @PostMapping("/verify-pin")
    public ResponseEntity<ApiResponse<StepUpTokenResponse>> verifyPin(
            @Valid @RequestBody VerifyPinRequest request,
            Authentication authentication
    ) {
        String phoneNumber = authentication.getName();
        StepUpTokenResponse response = authService.verifyPin(request, phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("PIN verified successfully", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", authService.refresh(request)));
    }

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkUser(@RequestParam String phoneNumber){
        return ResponseEntity.ok(ApiResponse.success("Success", authService.checkUser(phoneNumber)));
    }
}
