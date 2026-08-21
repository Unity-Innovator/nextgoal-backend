package com.example.genz.auth.service;

import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest user);
    RegisterResponse register(@Valid @RequestBody RegisterRequest request);
    Boolean checkUser(@RequestParam String phoneNumber);
}

