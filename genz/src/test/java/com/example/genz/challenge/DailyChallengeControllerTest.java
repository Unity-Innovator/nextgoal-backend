package com.example.genz.challenge;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.controller.DailyChallengeController;
import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.service.DailyChallengeService;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyChallengeControllerTest {

    @Mock
    private DailyChallengeService dailyChallengeService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DailyChallengeController dailyChallengeController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setPhoneNumber("0987654321");
    }

    @Test
    @DisplayName("getTodayChallenge returns challenge when available")
    void getTodayChallenge_WhenAvailable_ReturnsSuccess() {
        when(authentication.getName()).thenReturn("0987654321");
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(testUser));

        UserDailyChallengeResponse challengeResponse = UserDailyChallengeResponse.builder()
                .userDailyChallengeId(UUID.randomUUID())
                .challengeId(UUID.randomUUID())
                .title("Log Today's Coffee or Drink")
                .description("Track the exact cost of your drink.")
                .challengeType(ChallengeType.EXPENSE_LOG)
                .xpReward(25)
                .status(ChallengeStatus.PENDING)
                .challengeDate(LocalDate.now())
                .build();

        when(dailyChallengeService.getTodayChallenge(testUser)).thenReturn(Optional.of(challengeResponse));

        ResponseEntity<ApiResponse<UserDailyChallengeResponse>> response =
                dailyChallengeController.getTodayChallenge(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Today's daily challenge retrieved successfully", response.getBody().getMessage());
        assertEquals("Log Today's Coffee or Drink", response.getBody().getData().getTitle());
    }

    @Test
    @DisplayName("getTodayChallenge returns null data when no active challenge exists")
    void getTodayChallenge_WhenEmpty_ReturnsNoChallengeMessage() {
        when(authentication.getName()).thenReturn("0987654321");
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(testUser));
        when(dailyChallengeService.getTodayChallenge(testUser)).thenReturn(Optional.empty());

        ResponseEntity<ApiResponse<UserDailyChallengeResponse>> response =
                dailyChallengeController.getTodayChallenge(authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("No daily challenge is available today.", response.getBody().getMessage());
        assertNull(response.getBody().getData());
    }

    @Test
    @DisplayName("completeChallenge successfully marks challenge complete and returns response")
    void completeChallenge_Success() {
        UUID userDailyChallengeId = UUID.randomUUID();
        when(authentication.getName()).thenReturn("0987654321");
        when(userRepository.findByPhoneNumber("0987654321")).thenReturn(Optional.of(testUser));

        UserDailyChallengeResponse completedResponse = UserDailyChallengeResponse.builder()
                .userDailyChallengeId(userDailyChallengeId)
                .challengeId(UUID.randomUUID())
                .title("Zero-Spend Day Challenge")
                .challengeType(ChallengeType.EXPENSE_LOG)
                .xpReward(50)
                .status(ChallengeStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .challengeDate(LocalDate.now())
                .build();

        when(dailyChallengeService.completeChallenge(userDailyChallengeId, testUser)).thenReturn(completedResponse);

        ResponseEntity<ApiResponse<UserDailyChallengeResponse>> response =
                dailyChallengeController.completeChallenge(userDailyChallengeId, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Daily challenge completed successfully", response.getBody().getMessage());
        assertEquals(ChallengeStatus.COMPLETED, response.getBody().getData().getStatus());
    }

    @Test
    @DisplayName("Throws ResourceNoteFoundException when user is not authenticated")
    void whenAuthenticationIsNull_ThrowsException() {
        assertThrows(ResourceNoteFoundException.class, () -> {
            dailyChallengeController.getTodayChallenge(null);
        });
    }
}
