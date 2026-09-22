package com.example.genz.user;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.service.AchievementService;
import com.example.genz.user.controller.ProfileController;
import com.example.genz.user.dto.ProfileLevelResponse;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.dto.ProfileSummaryResponse;
import com.example.genz.user.dto.ProfileUserResponse;
import com.example.genz.user.service.ProfileService;
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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerTest {

    @Mock
    private ProfileService profileService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private AchievementService achievementService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ProfileController profileController;

    private User testUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .fullName("Chea Teng")
                .phoneNumber("012345678")
                .gender(Gender.MALE)
                .totalXp(350)
                .coinBalance(245)
                .build();
    }

    @Test
    @DisplayName("getMyProfile returns profile response for authenticated user")
    void getMyProfile_WhenAuthenticated_ReturnsProfile() {
        when(authentication.getName()).thenReturn("012345678");
        when(userRepository.findByPhoneNumber("012345678")).thenReturn(Optional.of(testUser));

        ProfileResponse profileResponse = ProfileResponse.builder()
                .user(ProfileUserResponse.builder()
                        .id(userId)
                        .fullName("Chea Teng")
                        .achievementUnlockedCount(4)
                        .build())
                .level(ProfileLevelResponse.builder()
                        .level(3)
                        .name("Goal Builder")
                        .totalXp(365)
                        .currentLevelXp(300)
                        .nextLevelXp(600)
                        .progressPercentage(21.67)
                        .completedAchievements(4)
                        .streak(5)
                        .build())
                .summary(ProfileSummaryResponse.builder()
                        .totalSaved(new BigDecimal("1250.00"))
                        .totalGoals(5L)
                        .coins(245)
                        .build())
                .build();

        when(profileService.getUserProfile(testUser)).thenReturn(profileResponse);

        ResponseEntity<ApiResponse<ProfileResponse>> responseEntity = profileController.getMyProfile(authentication);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("Profile retrieved successfully", responseEntity.getBody().getMessage());

        ProfileResponse data = responseEntity.getBody().getData();
        assertEquals("Chea Teng", data.getUser().getFullName());
        assertEquals(4, data.getUser().getAchievementUnlockedCount());
        assertEquals(3, data.getLevel().getLevel());
        assertEquals("Goal Builder", data.getLevel().getName());
        assertEquals(365, data.getLevel().getTotalXp());
        assertEquals(21.67, data.getLevel().getProgressPercentage());
        assertEquals(4, data.getLevel().getCompletedAchievements());
        assertEquals(5, data.getLevel().getStreak());
        assertEquals(new BigDecimal("1250.00"), data.getSummary().getTotalSaved());
        assertEquals(5L, data.getSummary().getTotalGoals());
        assertEquals(245, data.getSummary().getCoins());

        verify(profileService).getUserProfile(testUser);
    }

    @Test
    @DisplayName("getMyProfile throws exception when unauthenticated")
    void getMyProfile_WhenUnauthenticated_ThrowsException() {
        assertThrows(ResourceNoteFoundException.class, () -> profileController.getMyProfile(null));
    }

    @Test
    @DisplayName("getMyProfile throws exception when user not found in database")
    void getMyProfile_WhenUserNotFound_ThrowsException() {
        when(authentication.getName()).thenReturn("099999999");
        when(userRepository.findByPhoneNumber("099999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNoteFoundException.class, () -> profileController.getMyProfile(authentication));
    }

    @Test
    @DisplayName("getProfileV1 returns legacy profile response for authenticated user")
    void getProfileV1_WhenAuthenticated_ReturnsProfileV1() {
        when(authentication.getName()).thenReturn("012345678");
        when(userRepository.findByPhoneNumber("012345678")).thenReturn(Optional.of(testUser));
        when(goalRepository.countByOwner(testUser)).thenReturn(5L);
        when(achievementService.getUserRewardSummary(testUser)).thenReturn(
                UserRewardSummaryResponse.builder().level(3).build()
        );

        ResponseEntity<ApiResponse<ProfileController.ProfileV1Response>> responseEntity =
                profileController.getProfileV1(authentication);

        assertNotNull(responseEntity);
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertTrue(responseEntity.getBody().isSuccess());
        assertEquals("Profile retrieved", responseEntity.getBody().getMessage());

        ProfileController.ProfileV1Response data = responseEntity.getBody().getData();
        assertEquals("Chea Teng", data.name());
        assertEquals("012345678", data.phone());
        assertEquals(Gender.MALE.name(), data.gender());
        assertEquals(3, data.level());
        assertEquals(5L, data.goalsCount());
        assertEquals(245, data.coinsCount());
    }
}
