package com.example.genz.auth.service;

import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.dto.SendPhoneOtpRequest;
import com.example.genz.auth.dto.VerifyOtpRequest;
import com.example.genz.auth.dto.VerifyOtpResponse;

public interface OtpService {
    void sendOtp(SendOtpRequest request);
    void sendPhoneOtp(SendPhoneOtpRequest request);
    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);
}
