package com.example.genz.Controller;

import com.example.genz.DTO.DailyChallengeDTO.UserDailyChallengeResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Service.DailyChallengeService.DailyChallengeService;
import com.example.genz.exception.ApiResponse;
import com.example.genz.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

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

    @PostMapping("/{userDailyChallengeId}/complete")
    public ResponseEntity<ApiResponse<UserDailyChallengeResponse>> completeChallenge(
            @PathVariable UUID userDailyChallengeId,
            Authentication authentication) {
        User user = getUser(authentication);
        UserDailyChallengeResponse response = dailyChallengeService.completeChallenge(userDailyChallengeId, user);
        return ResponseEntity.ok(ApiResponse.success("Daily challenge completed successfully", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
