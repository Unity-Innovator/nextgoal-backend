package com.example.genz.settings.mapper;

import com.example.genz.settings.dto.UpdateUserSettingsRequest;
import com.example.genz.settings.dto.UserSettingsResponse;
import com.example.genz.settings.entity.UserSettings;
import org.springframework.stereotype.Component;

@Component
public class UserSettingsMapper {

    public UserSettingsResponse toUserSettingsResponse(UserSettings settings) {
        if (settings == null) {
            return null;
        }

        return UserSettingsResponse.builder()
                .language(settings.getLanguage())
                .theme(settings.getTheme())
                .billReminder(settings.getBillReminder())
                .avatarUrl(settings.getAvatarUrl())
                .goalAlert(settings.getGoalAlert())
                .lessonReminder(settings.getLessonReminder())
                .aiTips(settings.getAiTips())
                .dailyChallengeEnabled(settings.getDailyChallengeEnabled())
                .faceIdEnabled(settings.getFaceIdEnabled())
                .autoLockMinutes(settings.getAutoLockMinutes())
                .pushNotificationsEnabled(settings.getPushNotificationsEnabled())
                .smsNotificationsEnabled(settings.getSmsNotificationsEnabled())
                .emailNotificationsEnabled(settings.getEmailNotificationsEnabled())
                .profileVisibility(settings.getProfileVisibility())
                .showSavingsGoalsPublicly(settings.getShowSavingsGoalsPublicly())
                .showFullNameOnPosts(settings.getShowFullNameOnPosts())
                .allowCommentsOnPosts(settings.getAllowCommentsOnPosts())
                .preferredCurrency(settings.getPreferredCurrency())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }

    public void updateEntityFromRequest(UpdateUserSettingsRequest request, UserSettings settings) {
        if (request == null || settings == null) {
            return;
        }

        if (request.getLanguage() != null) settings.setLanguage(request.getLanguage());
        if (request.getTheme() != null) settings.setTheme(request.getTheme());
        if (request.getBillReminder() != null) settings.setBillReminder(request.getBillReminder());
        if (request.getAvatarUrl() != null) settings.setAvatarUrl(request.getAvatarUrl());
        if (request.getGoalAlert() != null) settings.setGoalAlert(request.getGoalAlert());
        if (request.getLessonReminder() != null) settings.setLessonReminder(request.getLessonReminder());
        if (request.getAiTips() != null) settings.setAiTips(request.getAiTips());
        if (request.getDailyChallengeEnabled() != null) settings.setDailyChallengeEnabled(request.getDailyChallengeEnabled());
        if (request.getFaceIdEnabled() != null) settings.setFaceIdEnabled(request.getFaceIdEnabled());
        if (request.getAutoLockMinutes() != null) settings.setAutoLockMinutes(request.getAutoLockMinutes());
        if (request.getPushNotificationsEnabled() != null) settings.setPushNotificationsEnabled(request.getPushNotificationsEnabled());
        if (request.getSmsNotificationsEnabled() != null) settings.setSmsNotificationsEnabled(request.getSmsNotificationsEnabled());
        if (request.getEmailNotificationsEnabled() != null) settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
        if (request.getProfileVisibility() != null) settings.setProfileVisibility(request.getProfileVisibility());
        if (request.getShowSavingsGoalsPublicly() != null) settings.setShowSavingsGoalsPublicly(request.getShowSavingsGoalsPublicly());
        if (request.getShowFullNameOnPosts() != null) settings.setShowFullNameOnPosts(request.getShowFullNameOnPosts());
        if (request.getAllowCommentsOnPosts() != null) settings.setAllowCommentsOnPosts(request.getAllowCommentsOnPosts());
        if (request.getPreferredCurrency() != null) settings.setPreferredCurrency(request.getPreferredCurrency());
    }
}
