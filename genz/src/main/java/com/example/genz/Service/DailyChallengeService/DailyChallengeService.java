package com.example.genz.service.DailyChallengeService;

import com.example.genz.dto.DailyChallengeDTO.UserDailyChallengeResponse;
import com.example.genz.entity.Auth.User;

import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeService {
    Optional<UserDailyChallengeResponse> getTodayChallenge(User user);
    UserDailyChallengeResponse completeChallenge(UUID userDailyChallengeId, User user);
}
