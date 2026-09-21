package com.example.genz.auth.controller;

import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.reward.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final UserRepository users;
    private final GoalRepository goals;
    private final AchievementService achievements;

    @GetMapping("/api/v1/profile")
    public ApiResponse<ProfileResponse> profile(Authentication authentication) {
        var user = users.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        var rewards = achievements.getUserRewardSummary(user);
        return ApiResponse.success("Profile retrieved", new ProfileResponse(
                user.getFullName(), user.getPhoneNumber(), user.getDateOfBirth(),
                user.getGender().name(), rewards.getLevel(), goals.countByOwner(user),
                user.getCoinBalance()));
    }

    public record ProfileResponse(String name, String phone, java.time.LocalDate birthday,
                                  String gender, Integer level, long goalsCount, Integer coinsCount) {}
}
