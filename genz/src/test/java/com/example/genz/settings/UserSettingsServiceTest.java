package com.example.genz.settings;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.settings.dto.UpdateUserSettingsRequest;
import com.example.genz.settings.dto.UserSettingsResponse;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.mapper.UserSettingsMapper;
import com.example.genz.settings.repository.UserSettingsRepository;
import com.example.genz.settings.service.UserSettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository userSettingsRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserSettingsMapper userSettingsMapper;

    @InjectMocks
    private UserSettingsServiceImpl userSettingsService;

    private User testUser;
    private UserSettings defaultSettings;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .fullName("Jane Smith")
                .phoneNumber("0987654321")
                .dateOfBirth(LocalDate.of(2002, 5, 10))
                .gender(Gender.FEMALE)
                .status("ACTIVE")
                .build();

        defaultSettings = UserSettings.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .language("en")
                .theme("LIGHT")
                .billReminder(true)
                .goalAlert(true)
                .lessonReminder(true)
                .aiTips(true)
                .dailyChallengeEnabled(true)
                .faceIdEnabled(false)
                .autoLockMinutes(5)
                .pushNotificationsEnabled(true)
                .build();
    }

    @Test
    @DisplayName("Should return user settings with dailyChallengeEnabled = true by default")
    void getUserSettings_ReturnsDefaultTrue() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUser(testUser)).thenReturn(Optional.of(defaultSettings));

        UserSettingsResponse response = userSettingsService.getUserSettings(testUser);

        assertNotNull(response);
        assertTrue(response.getDailyChallengeEnabled());
        assertEquals("en", response.getLanguage());
    }

    @Test
    @DisplayName("Should create default settings with dailyChallengeEnabled = true if none exist")
    void getUserSettings_CreatesNewIfNotExist() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSettingsResponse response = userSettingsService.getUserSettings(testUser);

        assertNotNull(response);
        assertTrue(response.getDailyChallengeEnabled());
    }

    @Test
    @DisplayName("Should update dailyChallengeEnabled setting from true to false")
    void updateUserSettings_DisableDailyChallenge() {
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userSettingsRepository.findByUser(testUser)).thenReturn(Optional.of(defaultSettings));
        when(userSettingsRepository.save(any(UserSettings.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserSettingsRequest request = UpdateUserSettingsRequest.builder()
                .dailyChallengeEnabled(false)
                .build();

        UserSettingsResponse response = userSettingsService.updateUserSettings(testUser, request);

        assertNotNull(response);
        assertFalse(response.getDailyChallengeEnabled());
        assertFalse(defaultSettings.getDailyChallengeEnabled());
    }
}
