package com.example.genz.settings.controller;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.settings.dto.UpdateUserSettingsRequest;
import com.example.genz.settings.dto.UserSettingsResponse;
import com.example.genz.settings.service.UserSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> getUserSettings(Authentication authentication) {
        User user = getUser(authentication);
        UserSettingsResponse response = userSettingsService.getUserSettings(user);
        return ResponseEntity.ok(ApiResponse.success("User settings retrieved successfully", response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateUserSettings(
            @RequestBody UpdateUserSettingsRequest request,
            Authentication authentication) {
        User user = getUser(authentication);
        UserSettingsResponse response = userSettingsService.updateUserSettings(user, request);
        return ResponseEntity.ok(ApiResponse.success("User settings updated successfully", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
