package com.example.genz.mapper;

import com.example.genz.DTO.AuthDTO.LoginResponse;
import com.example.genz.DTO.AuthDTO.RegisterRequest;
import com.example.genz.DTO.AuthDTO.RegisterResponse;
import com.example.genz.DTO.AuthDTO.UserResponse;
import com.example.genz.Entity.Auth.User;
import org.springframework.stereotype.Component;

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

    public User toEntity(RegisterRequest request, String passwordHash, String pinHash, String fullName) {
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
                .status("ACTIVE")
                .build();
    }

    public LoginResponse toLoginResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
    }

    public RegisterResponse toRegisterResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        return new RegisterResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900L,
                userResponse
        );
    }
}
