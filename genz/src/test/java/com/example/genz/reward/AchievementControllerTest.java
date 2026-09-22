package com.example.genz.reward;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.reward.controller.AchievementController;
import com.example.genz.reward.dto.AchievementResponse;
import com.example.genz.reward.dto.UserAchievementsResponse;
import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.reward.service.AchievementService;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AchievementControllerTest {

    @Mock
    private AchievementService achievementService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AchievementController achievementController;

    private User testUser;
    private AchievementResponse unlockedResponse;
    private AchievementResponse lockedResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Test User")
                .phoneNumber("012345678")
                .build();

        unlockedResponse = AchievementResponse.builder()
                .id(UUID.randomUUID())
                .name("First Goal Created")
                .category(BadgeCategory.SAVINGS_MILESTONES)
                .unlocked(true)
                .currentProgress(1L)
                .requirementValue(1)
                .xpReward(50)
                .progressPercentage(100)
                .build();

        lockedResponse = AchievementResponse.builder()
                .id(UUID.randomUUID())
                .name("Savings Master")
                .category(BadgeCategory.SAVINGS_MILESTONES)
                .unlocked(false)
                .currentProgress(0L)
                .requirementValue(3)
                .xpReward(200)
                .progressPercentage(0)
                .build();

        lenient().when(authentication.getName()).thenReturn("012345678");
        lenient().when(userRepository.findByPhoneNumber("012345678")).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("getUserAchievements returns single payload with total, completed, and achievement list")
    void testGetUserAchievements() {
        UserAchievementsResponse mockResponse = UserAchievementsResponse.builder()
                .total(2)
                .completed(1)
                .achievements(List.of(unlockedResponse, lockedResponse))
                .build();

        when(achievementService.getUserAchievements(testUser)).thenReturn(mockResponse);

        ResponseEntity<ApiResponse<UserAchievementsResponse>> responseEntity =
                achievementController.getUserAchievements(authentication);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());

        UserAchievementsResponse data = responseEntity.getBody().getData();
        assertEquals(2, data.getTotal());
        assertEquals(1, data.getCompleted());
        assertEquals(2, data.getAchievements().size());

        // Verify JSON aliases / getters
        AchievementResponse item0 = data.getAchievements().get(0);
        assertTrue(item0.getCompleted());
        assertEquals(1L, item0.getProgress());
        assertEquals(1, item0.getTarget());
        assertEquals(50, item0.getRewardXp());

        verify(achievementService).getUserAchievements(testUser);
    }

    @Test
    @DisplayName("getUserAchievements throws exception when unauthenticated")
    void testGetUserAchievements_Unauthenticated() {
        assertThrows(ResourceNoteFoundException.class, () ->
                achievementController.getUserAchievements(null));
    }
}
