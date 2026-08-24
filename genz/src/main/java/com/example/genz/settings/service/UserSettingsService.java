package com.example.genz.settings.service;

import com.example.genz.auth.entity.User;
import com.example.genz.settings.dto.UpdateUserSettingsRequest;
import com.example.genz.settings.dto.UserSettingsResponse;
import com.example.genz.settings.entity.UserSettings;

public interface UserSettingsService {

    UserSettingsResponse getUserSettings(User user);

    UserSettingsResponse updateUserSettings(User user, UpdateUserSettingsRequest request);

    UserSettings getOrCreateUserSettings(User user);
}
