package com.example.genz.auth.controller;

import com.example.genz.auth.dto.*;
import com.example.genz.auth.service.AuthService;
import com.example.genz.auth.service.OtpService;
import com.example.genz.exception.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(new ApiResponse<>("success", authService.register(request)));
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

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> checkUser(@RequestParam String phoneNumber){
        return ResponseEntity.ok(new ApiResponse<>("Success",authService.checkUser(phoneNumber)));
    }
}
