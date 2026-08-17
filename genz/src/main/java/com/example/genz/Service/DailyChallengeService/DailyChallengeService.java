package com.example.genz.Service.DailyChallengeService;

import com.example.genz.DTO.DailyChallengeDTO.UserDailyChallengeResponse;
import com.example.genz.Entity.Auth.User;

import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeService {
    Optional<UserDailyChallengeResponse> getTodayChallenge(User user);
    UserDailyChallengeResponse completeChallenge(UUID userDailyChallengeId, User user);
}
