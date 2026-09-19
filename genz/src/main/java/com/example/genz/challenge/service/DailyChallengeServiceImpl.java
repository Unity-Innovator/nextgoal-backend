package com.example.genz.challenge.service;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.mapper.DailyChallengeMapper;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyChallengeServiceImpl implements DailyChallengeService {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final UserDailyChallengeRepository userDailyChallengeRepository;
    private final ChallengeCompletionService challengeCompletionService;
    private final DailyChallengeMapper dailyChallengeMapper;

    @Override
    @Transactional
    public Optional<UserDailyChallengeResponse> getTodayChallenge(User user) {
        LocalDate today = LocalDate.now();
        int streak = challengeCompletionService.calculateStreak(user);

        List<UserDailyChallenge> todayUserChallenges = userDailyChallengeRepository.findAllByUserAndChallengeDate(user, today);
        if (!todayUserChallenges.isEmpty()) {
            // Prioritize completed challenge if present, otherwise first assigned
            UserDailyChallenge display = todayUserChallenges.stream()
                    .filter(c -> c.getStatus() == ChallengeStatus.COMPLETED)
                    .findFirst()
                    .orElse(todayUserChallenges.get(0));
            return Optional.of(dailyChallengeMapper.toUserDailyChallengeResponse(display, streak));
        }

        List<DailyChallenge> activeChallenges = dailyChallengeRepository.findByIsActiveTrue();
        if (activeChallenges.isEmpty()) {
            log.info("No active daily challenge templates available for user {}", user.getId());
            return Optional.empty();
        }

        DailyChallenge selectedChallenge = activeChallenges.get(ThreadLocalRandom.current().nextInt(activeChallenges.size()));

        UserDailyChallenge newChallenge = UserDailyChallenge.builder()
                .user(user)
                .challenge(selectedChallenge)
                .challengeDate(today)
                .status(ChallengeStatus.PENDING)
                .build();

        try {
            newChallenge = userDailyChallengeRepository.saveAndFlush(newChallenge);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Race condition creating challenge for user {} on date {}. Retrieving existing record.", user.getId(), today);
            newChallenge = userDailyChallengeRepository.findByUserAndChallengeAndChallengeDate(user, selectedChallenge, today)
                    .orElseGet(() -> userDailyChallengeRepository.findAllByUserAndChallengeDate(user, today).stream().findFirst().orElseThrow(() -> ex));
        }

        return Optional.of(dailyChallengeMapper.toUserDailyChallengeResponse(newChallenge, streak));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDailyChallengeResponse> getAllTodayChallenges(User user) {
        LocalDate today = LocalDate.now();
        int streak = challengeCompletionService.calculateStreak(user);

        List<UserDailyChallenge> todayUserChallenges = userDailyChallengeRepository.findAllByUserAndChallengeDate(user, today);
        return todayUserChallenges.stream()
                .map(udc -> dailyChallengeMapper.toUserDailyChallengeResponse(udc, streak))
                .toList();
    }

    @Override
    @Transactional
    public UserDailyChallengeResponse completeChallenge(UUID userDailyChallengeId, User user) {
        UserDailyChallenge completed = challengeCompletionService.completeManualChallenge(userDailyChallengeId, user);
        int streak = challengeCompletionService.calculateStreak(user);
        return dailyChallengeMapper.toUserDailyChallengeResponse(completed, streak);
    }
}
