package com.example.genz.reward.controller;

import com.example.genz.reward.dto.AchievementResponse;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.dto.XpTransactionResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.reward.service.AchievementService;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AchievementController {

    private final AchievementService achievementService;
    private final UserRepository userRepository;

    @GetMapping("/api/achievements")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getAllAchievements(Authentication authentication) {
        User user = getUser(authentication);
        List<AchievementResponse> response = achievementService.getAllAchievementsForUser(user);
        return ResponseEntity.ok(ApiResponse.success("Achievements retrieved successfully", response));
    }

    @GetMapping("/api/achievements/me")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getMyAchievements(Authentication authentication) {
        User user = getUser(authentication);
        List<AchievementResponse> response = achievementService.getAllAchievementsForUser(user);
        return ResponseEntity.ok(ApiResponse.success("My achievement progress retrieved successfully", response));
    }

    @GetMapping("/api/achievements/unlocked")
    public ResponseEntity<ApiResponse<List<AchievementResponse>>> getUnlockedAchievements(Authentication authentication) {
        User user = getUser(authentication);
        List<AchievementResponse> response = achievementService.getUnlockedAchievementsForUser(user);
        return ResponseEntity.ok(ApiResponse.success("Unlocked achievements retrieved successfully", response));
    }

    @GetMapping("/api/achievements/categories")
    public ResponseEntity<ApiResponse<BadgeCategory[]>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success("Badge categories retrieved successfully", BadgeCategory.values()));
    }

    @GetMapping("/api/rewards/me")
    public ResponseEntity<ApiResponse<UserRewardSummaryResponse>> getRewardSummary(Authentication authentication) {
        User user = getUser(authentication);
        UserRewardSummaryResponse response = achievementService.getUserRewardSummary(user);
        return ResponseEntity.ok(ApiResponse.success("User reward summary retrieved successfully", response));
    }

    @GetMapping("/api/rewards/xp-history")
    public ResponseEntity<ApiResponse<List<XpTransactionResponse>>> getXpHistory(Authentication authentication) {
        User user = getUser(authentication);
        List<XpTransactionResponse> response = achievementService.getXpHistory(user);
        return ResponseEntity.ok(ApiResponse.success("XP history retrieved successfully", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
