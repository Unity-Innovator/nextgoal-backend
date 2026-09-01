package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.SendOtpRequest;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.OtpRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.security.jwt.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class OtpServiceImplTest {

    @Test
    void productionSafeDefaultDoesNotPersistOrLogAnUndeliverableOtp() {
        OtpRepository otpRepository = mock(OtpRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        OtpProperties properties = new OtpProperties();
        JwtUtil jwtUtil = mock(JwtUtil.class);
        OtpServiceImpl service = new OtpServiceImpl(
                otpRepository,
                userRepository,
                properties,
                jwtUtil
        );
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber("0123456789");

        OtpException exception = assertThrows(OtpException.class, () -> service.sendOtp(request));

        assertEquals("OTP delivery is not configured", exception.getMessage());
        verifyNoInteractions(otpRepository, userRepository, jwtUtil);
    }
}
