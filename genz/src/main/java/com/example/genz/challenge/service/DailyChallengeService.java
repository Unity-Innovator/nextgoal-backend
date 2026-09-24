package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.dto.UserDailyChallengeResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyChallengeService {
    Optional<UserDailyChallengeResponse> getTodayChallenge(User user);
    List<UserDailyChallengeResponse> getAllTodayChallenges(User user);
    UserDailyChallengeResponse completeChallenge(UUID userDailyChallengeId, User user);
}
