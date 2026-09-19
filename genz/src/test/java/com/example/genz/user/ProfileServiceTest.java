package com.example.genz.user;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.level.service.LevelService;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.mapper.ProfileMapper;
import com.example.genz.user.service.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private LevelService levelService;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private GoalContributionRepository goalContributionRepository;

    @Spy
    private ProfileMapper profileMapper = new ProfileMapper();

    @InjectMocks
    private ProfileServiceImpl profileService;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .fullName("Chea Teng")
                .phoneNumber("012345678")
                .totalXp(350)
                .coinBalance(245)
                .build();
    }

    @Test
    @DisplayName("Should retrieve complete user profile with level, total saved, total goals, and coins")
    void testGetUserProfileSuccess() {
        ProfileLevelResponse levelResponse = ProfileLevelResponse.builder()
                .level(3)
                .name("Goal Builder")
                .xp(350)
                .currentLevelXp(300)
                .nextLevelXp(600)
                .progressPercentage(16.67)
                .build();

        when(levelService.calculateLevelProgress(350)).thenReturn(levelResponse);
        when(goalContributionRepository.sumAmountByUserId(userId)).thenReturn(new BigDecimal("1250.00"));
        when(goalRepository.countByOwner(testUser)).thenReturn(5L);

        ProfileResponse response = profileService.getUserProfile(testUser);

        assertNotNull(response);
        assertNotNull(response.getUser());
        assertEquals(userId, response.getUser().getId());
        assertEquals("Chea Teng", response.getUser().getFullName());

        assertNotNull(response.getLevel());
        assertEquals(3, response.getLevel().getLevel());
        assertEquals("Goal Builder", response.getLevel().getName());
        assertEquals(350, response.getLevel().getXp());
        assertEquals(300, response.getLevel().getCurrentLevelXp());
        assertEquals(600, response.getLevel().getNextLevelXp());
        assertEquals(16.67, response.getLevel().getProgressPercentage());

        assertNotNull(response.getSummary());
        assertEquals(new BigDecimal("1250.00"), response.getSummary().getTotalSaved());
        assertEquals(5L, response.getSummary().getTotalGoals());
        assertEquals(245, response.getSummary().getCoins());

        verify(levelService).calculateLevelProgress(350);
        verify(goalContributionRepository).sumAmountByUserId(userId);
        verify(goalRepository).countByOwner(testUser);
    }

    @Test
    @DisplayName("Should handle null contributions and zero goals/coins gracefully")
    void testGetUserProfileWithZeroValues() {
        User newUser = User.builder()
                .id(userId)
                .fullName("New User")
                .totalXp(0)
                .coinBalance(0)
                .build();

        ProfileLevelResponse levelResponse = ProfileLevelResponse.builder()
                .level(1)
                .name("Rookie Saver")
                .xp(0)
                .currentLevelXp(0)
                .nextLevelXp(100)
                .progressPercentage(0.0)
                .build();

        when(levelService.calculateLevelProgress(0)).thenReturn(levelResponse);
        when(goalContributionRepository.sumAmountByUserId(userId)).thenReturn(null);
        when(goalRepository.countByOwner(newUser)).thenReturn(0L);

        ProfileResponse response = profileService.getUserProfile(newUser);

        assertNotNull(response);
        assertEquals(new BigDecimal("0.00"), response.getSummary().getTotalSaved());
        assertEquals(0L, response.getSummary().getTotalGoals());
        assertEquals(0, response.getSummary().getCoins());
    }

    @Test
    @DisplayName("Total saved must include contributions across multiple goals")
    void testTotalSavedAcrossMultipleGoals() {
        // Goal A: $500, Goal B: $300, Goal C: $200 -> total $1000
        when(goalContributionRepository.sumAmountByUserId(userId)).thenReturn(new BigDecimal("1000.0000"));
        when(levelService.calculateLevelProgress(anyInt())).thenReturn(mock(ProfileLevelResponse.class));
        when(goalRepository.countByOwner(testUser)).thenReturn(3L);

        ProfileResponse response = profileService.getUserProfile(testUser);

        assertNotNull(response.getSummary());
        assertEquals(new BigDecimal("1000.00"), response.getSummary().getTotalSaved());
    }

    @Test
    @DisplayName("Total goals must count all goals belonging to user, including non-active goals")
    void testTotalGoalsIncludesAllStatuses() {
        // 2 Active, 1 Completed, 1 Cancelled = 4 total goals
        when(goalRepository.countByOwner(testUser)).thenReturn(4L);
        when(levelService.calculateLevelProgress(anyInt())).thenReturn(mock(ProfileLevelResponse.class));

        ProfileResponse response = profileService.getUserProfile(testUser);

        assertNotNull(response.getSummary());
        assertEquals(4L, response.getSummary().getTotalGoals());
    }

    @Test
    @DisplayName("Throws exception when user is null")
    void testNullUser() {
        assertThrows(IllegalArgumentException.class, () -> profileService.getUserProfile(null));
    }
}
