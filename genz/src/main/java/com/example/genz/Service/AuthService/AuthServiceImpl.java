package com.example.genz.Service.AuthService;

import com.example.genz.DTO.AuthDTO.*;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Exception.ResourceNoteFoundException;
import com.example.genz.Entity.Wallet.Wallet;
import com.example.genz.Enums.Currency;
import com.example.genz.Repository.QrCodeRepository.WalletRepository;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.Repository.AuthRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        System.out.println("Before authentication");
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword())
        );
        System.out.println("After authentication");

        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new RuntimeException("User not found"));
        System.out.println("User found");
        String phoneNumber=user.getPhoneNumber();

        String jwt = jwtUtil.generateAccessToken(phoneNumber);

        String refreshToken = jwtUtil.generateRefreshToken(phoneNumber);

        UserResponse userResponse=UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .status(user.getStatus())
                .build();

        return LoginResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        final String fullName = request.getFirstName() + " " + request.getLastName();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())){
            throw new RuntimeException("Phone number already exists");
        }

        User user = User.builder()
                .fullName(fullName)
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .pinHash(passwordEncoder.encode(request.getPin()))
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .status("ACTIVE")
                .build();

        user =  userRepository.save(user);
        System.out.println("User saved");

        List<Wallet> defaultWallets = List.of(
                Wallet.builder()
                        .users(user)
                        .balance(BigDecimal.ZERO)
                        .accountNumber("USD-" + user.getPhoneNumber())
                        .currency(Currency.USD)
                        .build(),
                Wallet.builder()
                        .users(user)
                        .balance(BigDecimal.ZERO)
                        .accountNumber("KHR-" + user.getPhoneNumber())
                        .currency(Currency.KHR)
                        .build()
        );

        walletRepository.saveAll(defaultWallets);

        final String accessToken = jwtUtil.generateAccessToken(user.getPhoneNumber());
        final String refreshToken = jwtUtil.generateRefreshToken(user.getPhoneNumber());

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getStatus()
        );

        return new RegisterResponse(
                accessToken,
                refreshToken,
                "Bearer",
                900L,
                userResponse
        );
    }

    @Override
    public Boolean checkUser(String phoneNumber) {
        User user=userRepository.findByPhoneNumber(phoneNumber).orElseThrow(()->new ResourceNoteFoundException("User not found"));
        return user !=null;
    }
}
