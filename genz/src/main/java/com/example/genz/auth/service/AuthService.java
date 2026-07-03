package com.example.genz.auth.service;

import com.example.genz.auth.dto.LoginRequest;
import com.example.genz.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest user);
}

