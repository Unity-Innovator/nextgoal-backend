package com.example.genz.challenge.service;

import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.reward.entity.XpTransaction;
import com.example.genz.reward.enums.ActivityType;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.mapper.DailyChallengeMapper;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.reward.service.AchievementService;
import com.example.genz.common.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyChallengeServiceImpl implements DailyChallengeService {

    private final DailyChallengeRepository dailyChallengeRepository;
    private final UserDailyChallengeRepository userDailyChallengeRepository;
    private final UserRewardRepository userRewardRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AchievementService achievementService;
    private final DailyChallengeMapper dailyChallengeMapper;

    @Override
    @Transactional
    public Optional<UserDailyChallengeResponse> getTodayChallenge(User user) {
        LocalDate today = LocalDate.now();

        Optional<UserDailyChallenge> existingOpt = userDailyChallengeRepository.findByUserAndChallengeDate(user, today);
        if (existingOpt.isPresent()) {
            return Optional.of(dailyChallengeMapper.toUserDailyChallengeResponse(existingOpt.get()));
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
            log.warn("Concurrency race condition detected for user {} on date {}. Retrieving existing record.", user.getId(), today);
            newChallenge = userDailyChallengeRepository.findByUserAndChallengeDate(user, today)
                    .orElseThrow(() -> ex);
        }

        return Optional.of(dailyChallengeMapper.toUserDailyChallengeResponse(newChallenge));
    }

    @Override
    @Transactional
    public UserDailyChallengeResponse completeChallenge(UUID userDailyChallengeId, User user) {
        UserDailyChallenge userDailyChallenge = userDailyChallengeRepository.findById(userDailyChallengeId)
                .orElseThrow(() -> new ResourceNoteFoundException("Daily challenge assignment not found with id: " + userDailyChallengeId));

        if (!userDailyChallenge.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You are not authorized to complete another user's daily challenge.");
        }

        if (userDailyChallenge.getStatus() == ChallengeStatus.COMPLETED || userDailyChallenge.getStatus() == ChallengeStatus.DONE) {
            log.info("Daily challenge {} is already completed for user {}", userDailyChallengeId, user.getId());
            return dailyChallengeMapper.toUserDailyChallengeResponse(userDailyChallenge);
        }

        userDailyChallenge.setStatus(ChallengeStatus.COMPLETED);
        userDailyChallenge.setCompletedAt(LocalDateTime.now());
        userDailyChallenge = userDailyChallengeRepository.save(userDailyChallenge);

        DailyChallenge challengeTemplate = userDailyChallenge.getChallenge();
        int xpReward = (challengeTemplate != null && challengeTemplate.getXpReward() != null) ? challengeTemplate.getXpReward() : 0;

        if (xpReward > 0 && !xpTransactionRepository.existsByUserAndActivityTypeAndReferenceId(user, ActivityType.DAILY_CHALLENGE, userDailyChallenge.getId())) {
            UserReward userReward = achievementService.getOrCreateUserReward(user);
            userReward.addXp(xpReward);
            userRewardRepository.save(userReward);

            XpTransaction xpTransaction = XpTransaction.builder()
                    .user(user)
                    .activityType(ActivityType.DAILY_CHALLENGE)
                    .referenceId(userDailyChallenge.getId())
                    .points(xpReward)
                    .build();
            xpTransactionRepository.save(xpTransaction);
        }

        return dailyChallengeMapper.toUserDailyChallengeResponse(userDailyChallenge);
    }
}
