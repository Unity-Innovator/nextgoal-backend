package com.example.genz.service.AuthService;

import org.springframework.web.bind.annotation.RequestBody;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.genz.dto.AuthDTO.LoginRequest;
import com.example.genz.dto.AuthDTO.LoginResponse;
import com.example.genz.dto.AuthDTO.RegisterRequest;
import com.example.genz.dto.AuthDTO.RegisterResponse;

public interface AuthService {
    LoginResponse login(LoginRequest user);
    RegisterResponse register(@Valid @RequestBody RegisterRequest request);
    Boolean checkUser(@RequestParam String phoneNumber);
}

