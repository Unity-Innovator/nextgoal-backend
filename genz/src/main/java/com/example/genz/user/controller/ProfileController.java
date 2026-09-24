package com.example.genz.user.controller;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.service.AchievementService;
import com.example.genz.user.dto.ProfileResponse;
import com.example.genz.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final AchievementService achievementService;

    @GetMapping("/api/users/me/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMyProfile(Authentication authentication) {
        User user = getUser(authentication);
        ProfileResponse response = profileService.getUserProfile(user);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @GetMapping("/api/v1/profile")
    public ResponseEntity<ApiResponse<ProfileV1Response>> getProfileV1(Authentication authentication) {
        User user = getUser(authentication);
        UserRewardSummaryResponse rewards = achievementService.getUserRewardSummary(user);
        ProfileV1Response response = new ProfileV1Response(
                user.getFullName(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getGender() != null ? user.getGender().name() : null,
                rewards != null ? rewards.getLevel() : null,
                goalRepository.countByOwner(user),
                user.getCoinBalance()
        );
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        if (authentication.getPrincipal() instanceof User u) {
            return userRepository.findById(u.getId())
                    .orElseThrow(() -> new ResourceNoteFoundException("User not found with ID: " + u.getId()));
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }

    public record ProfileV1Response(String name, String phone, LocalDate birthday,
                                    String gender, Integer level, long goalsCount, Integer coinsCount) {}
}

