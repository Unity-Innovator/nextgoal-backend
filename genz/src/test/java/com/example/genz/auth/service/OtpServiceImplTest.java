package com.example.genz.auth.service;

import com.example.genz.auth.config.OtpProperties;
import com.example.genz.auth.dto.*;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.exception.OtpException;
import com.example.genz.auth.repository.*;
import com.example.genz.auth.mapper.UserMapper;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class OtpServiceImplTest {
    private final RedisOtpStore store = mock(RedisOtpStore.class);
    private final UserRepository users = mock(UserRepository.class);
    private final VerificationTokenRepository tokens = mock(VerificationTokenRepository.class);
    private final PlasGateSmsSender sender = mock(PlasGateSmsSender.class);
    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private OtpHasher hasher;
    private OtpServiceImpl service;
    private final String phone = "012345678";

    @BeforeEach
    void setUp() {
        OtpProperties properties = new OtpProperties();
        properties.setSecret("test-only-otp-secret-at-least-32-characters");
        hasher = new OtpHasher(properties);
        service = new OtpServiceImpl(store, users, tokens, hasher, sender, jwtUtil, userMapper);
    }

    @Test
    void sendsSixDigitsAndStoresOnlyBoundHashBeforeDelivery() {
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber(phone);
        service.sendOtp(request);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(sender).sendOtp(eq(phone), raw.capture());
        assertTrue(raw.getValue().matches("[0-9]{6}"));
        var order = inOrder(store, sender);
        order.verify(store).save(phone, OtpPurpose.REGISTRATION,
                hasher.hash(phone, OtpPurpose.REGISTRATION, raw.getValue()));
        order.verify(sender).sendOtp(phone, raw.getValue());
    }

    @Test
    void deliveryFailureRemovesOnlyMatchingChallenge() {
        doThrow(new OtpException("delivery failed")).when(sender).sendOtp(anyString(), anyString());
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber(phone);
        assertThrows(OtpException.class, () -> service.sendOtp(request));
        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(store).save(eq(phone), eq(OtpPurpose.REGISTRATION), hash.capture());
        verify(store).removeIfMatching(phone, hash.getValue());
    }

    @Test
    void cooldownFailureDoesNotSendSms() {
        doThrow(new OtpException("cooldown")).when(store).save(anyString(), any(), anyString());
        SendOtpRequest request = new SendOtpRequest();
        request.setPhoneNumber(phone);
        assertThrows(OtpException.class, () -> service.sendOtp(request));
        verifyNoInteractions(sender);
    }

    @Test
    void consumesOtpBeforeIssuingHashedVerificationToken() {
        VerifyOtpRequest request = request();
        VerifyOtpResponse response = service.verifyOtp(request);
        ArgumentCaptor<VerificationToken> token = ArgumentCaptor.forClass(VerificationToken.class);
        var order = inOrder(store, tokens);
        order.verify(store).consume(phone, OtpPurpose.REGISTRATION,
                hasher.hash(phone, OtpPurpose.REGISTRATION, "123456"));
        order.verify(tokens).findByPhoneNumberAndUsedFalse(phone);
        order.verify(tokens).save(token.capture());
        assertEquals(TokenHashUtil.sha256Hex(response.getVerificationToken()), token.getValue().getTokenHash());
        assertEquals(OtpPurpose.REGISTRATION, token.getValue().getPurpose());
    }

    @Test
    void invalidOrReplayedOtpDoesNotIssueToken() {
        doThrow(new OtpException("invalid")).when(store).consume(anyString(), any(), anyString());
        assertThrows(OtpException.class, () -> service.verifyOtp(request()));
        verifyNoInteractions(tokens);
    }

    @Test
    void loginOtpReturnsAccessAndRefreshTokensWithoutCreatingVerificationToken() {
        var user = com.example.genz.auth.entity.User.builder()
                .phoneNumber(phone)
                .status("ACTIVE")
                .build();
        when(users.findByPhoneNumber(phone)).thenReturn(java.util.Optional.of(user));
        when(jwtUtil.generateAccessToken(phone)).thenReturn("access");
        when(jwtUtil.generateRefreshToken(phone)).thenReturn("refresh");
        when(jwtUtil.getAccessExpirationSeconds()).thenReturn(900L);
        var userResponse = UserResponse.builder().phoneNumber(phone).build();
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        SendOtpRequest send = new SendOtpRequest();
        send.setPhoneNumber(phone);
        send.setPurpose(OtpPurpose.LOGIN);
        service.sendOtp(send);
        ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
        verify(sender).sendOtp(eq(phone), raw.capture());

        VerifyOtpRequest verify = request();
        verify.setPurpose(OtpPurpose.LOGIN);
        verify.setOtp(raw.getValue());
        VerifyOtpResponse response = service.verifyOtp(verify);

        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        assertEquals(900L, response.getExpiresIn());
        assertEquals(userResponse, response.getUser());
        verify(tokens, never()).save(any());
    }

    @Test
    void hashesAreBoundToPhonePurposeAndSecret() {
        String hash = hasher.hash(phone, OtpPurpose.REGISTRATION, "123456");
        assertEquals(64, hash.length());
        assertNotEquals(hash, hasher.hash("098765432", OtpPurpose.REGISTRATION, "123456"));
        assertNotEquals(hash, hasher.hash(phone, OtpPurpose.PASSWORD_RESET, "123456"));
        assertNotEquals(hash, TokenHashUtil.sha256Hex("123456"));
        assertThrows(IllegalStateException.class,
                () -> new OtpHasher(new OtpProperties()).hash(phone, OtpPurpose.REGISTRATION, "123456"));
    }

    private VerifyOtpRequest request() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setPhoneNumber(phone);
        request.setPurpose(OtpPurpose.REGISTRATION);
        request.setOtp("123456");
        return request;
    }
}
