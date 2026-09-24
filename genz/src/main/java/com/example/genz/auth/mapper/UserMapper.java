package com.example.genz.auth.mapper;

import org.springframework.stereotype.Component;

import com.example.genz.auth.dto.LoginResponse;
import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.dto.UserResponse;
import com.example.genz.auth.entity.User;

@Component
public class UserMapper {
    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .status(user.getStatus())
                .build();
    }

    public User toEntity(
        RegisterRequest request, 
        String passwordHash, 
        String pinHash, 
        String fullName
    ) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .fullName(fullName)
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordHash)
                .pinHash(pinHash)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .build();
    }

    public LoginResponse toLoginResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserResponse userResponse
    ) {
        return new LoginResponse(
            accessToken, 
            refreshToken, 
            "Bearer", 
            expiresIn, 
            userResponse
        );
    }

    public RegisterResponse toRegisterResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserResponse userResponse
    ) {
        return new RegisterResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn,
                userResponse
        );
    }
}
