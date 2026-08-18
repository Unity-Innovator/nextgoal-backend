package com.example.genz.service.AuthService;

import com.example.genz.mapper.UserMapper;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.dto.AuthDTO.*;
import com.example.genz.entity.Auth.User;
import com.example.genz.exception.ResourceNoteFoundException;
import com.example.genz.repository.AuthRepository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String phoneNumber = user.getPhoneNumber();
        String jwt = jwtUtil.generateAccessToken(phoneNumber);
        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);

        UserResponse userResponse = userMapper.toUserResponse(user);
        return userMapper.toLoginResponse(jwt, refreshToken, userResponse);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        final String fullName = request.getFirstName() + " " + request.getLastName();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new RuntimeException("Phone number already exists");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        String pinHash = passwordEncoder.encode(request.getPin());

        User user = userMapper.toEntity(request, passwordHash, pinHash, fullName);
        user = userRepository.save(user);

        final String accessToken = jwtUtil.generateAccessToken(user.getPhoneNumber());
        final String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());

        UserResponse userResponse = userMapper.toUserResponse(user);
        return userMapper.toRegisterResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public Boolean checkUser(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
        return user != null;
    }
}
