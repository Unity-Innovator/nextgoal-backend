package com.example.genz.challenge.controller;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.challenge.service.DailyChallengeService;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/daily-challenges")
@RequiredArgsConstructor
public class DailyChallengeController {

    private final DailyChallengeService dailyChallengeService;
    private final UserRepository userRepository;

    @GetMapping("/today")
    public ResponseEntity<ApiResponse<UserDailyChallengeResponse>> getTodayChallenge(Authentication authentication) {
        User user = getUser(authentication);
        Optional<UserDailyChallengeResponse> responseOpt = dailyChallengeService.getTodayChallenge(user);
        if (responseOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No daily challenge is available today.", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Today's daily challenge retrieved successfully", responseOpt.get()));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
