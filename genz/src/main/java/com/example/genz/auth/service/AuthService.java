package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface AuthService {
    LoginResponse login(LoginRequest user);
    RegisterResponse register(@Valid @RequestBody RegisterRequest request);
    Boolean chekcUser(@RequestParam String phoneNumber);
}

