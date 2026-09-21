package com.example.genz.reward.controller;

import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.reward.service.AchievementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeDashboardController {
    private final UserRepository users;
    private final AchievementService achievements;

    @GetMapping("/api/v1/home/dashboard")
    public ApiResponse<HeaderResponse> dashboard(Authentication authentication) {
        var user = users.findByPhoneNumber(authentication.getName()).orElseThrow();
        var rewards = achievements.getUserRewardSummary(user);
        return ApiResponse.success("Dashboard retrieved",
                new HeaderResponse(user.getFullName(), rewards.getLevel()));
    }

    public record HeaderResponse(String userName, Integer userLevel) {}
}
