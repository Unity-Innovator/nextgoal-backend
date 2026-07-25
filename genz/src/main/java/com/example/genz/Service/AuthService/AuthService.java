package com.example.genz.Service.AuthService;

import org.springframework.web.bind.annotation.RequestBody;

import com.example.genz.DTO.AuthDTO.LoginRequest;
import com.example.genz.DTO.AuthDTO.LoginResponse;
import com.example.genz.DTO.AuthDTO.RegisterRequest;
import com.example.genz.DTO.AuthDTO.RegisterResponse;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;

public interface AuthService {
    LoginResponse login(LoginRequest user);
    RegisterResponse register(@Valid @RequestBody RegisterRequest request);
    Boolean checkUser(@RequestParam String phoneNumber);
}

