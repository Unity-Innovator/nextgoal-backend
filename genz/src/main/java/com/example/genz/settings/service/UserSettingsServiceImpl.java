package com.example.genz.settings.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.community.post.enums.PostVisibility;
import com.example.genz.settings.dto.UpdateUserSettingsRequest;
import com.example.genz.settings.dto.UserSettingsResponse;
import com.example.genz.settings.entity.UserSettings;
import com.example.genz.settings.mapper.UserSettingsMapper;
import com.example.genz.settings.repository.UserSettingsRepository;
import com.example.genz.transaction.enums.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSettingsServiceImpl implements UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final UserRepository userRepository;
    private final UserSettingsMapper userSettingsMapper;

    @Override
    @Transactional
    public UserSettingsResponse getUserSettings(User user) {
        User managedUser = getUserOrThrow(user);
        UserSettings settings = getOrCreateUserSettings(managedUser);
        return userSettingsMapper.toUserSettingsResponse(settings);
    }

    @Override
    @Transactional
    public UserSettingsResponse updateUserSettings(User user, UpdateUserSettingsRequest request) {
        User managedUser = getUserOrThrow(user);
        UserSettings settings = getOrCreateUserSettings(managedUser);
        userSettingsMapper.updateEntityFromRequest(request, settings);
        settings = userSettingsRepository.save(settings);
        return userSettingsMapper.toUserSettingsResponse(settings);
    }

    @Override
    @Transactional
    public UserSettings getOrCreateUserSettings(User user) {
        return userSettingsRepository.findByUser(user)
                .orElseGet(() -> userSettingsRepository.save(UserSettings.builder()
                        .user(user)
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
                        .smsNotificationsEnabled(false)
                        .emailNotificationsEnabled(true)
                        .profileVisibility(PostVisibility.PUBLIC)
                        .showSavingsGoalsPublicly(false)
                        .showFullNameOnPosts(true)
                        .allowCommentsOnPosts(true)
                        .preferredCurrency(Currency.USD)
                        .build()));
    }

    private User getUserOrThrow(User user) {
        if (user == null || user.getId() == null) {
            throw new ResourceNoteFoundException("User not authenticated.");
        }
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with id: " + user.getId()));
    }
}
