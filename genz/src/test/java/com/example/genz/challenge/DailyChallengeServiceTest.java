package com.example.genz.challenge;

import com.example.genz.challenge.dto.UserDailyChallengeResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.reward.entity.UserReward;
import com.example.genz.challenge.enums.ChallengeStatus;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.repository.XpTransactionRepository;
import com.example.genz.challenge.service.DailyChallengeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import com.example.genz.challenge.entity.ChallengeRule;
import com.example.genz.challenge.enums.ChallengeRuleType;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_daily_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class DailyChallengeServiceTest {

    @Autowired
    private DailyChallengeService dailyChallengeService;

    @Autowired
    private DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRewardRepository userRewardRepository;

    @Autowired
    private XpTransactionRepository xpTransactionRepository;

    private User userA;
    private User userB;
    private DailyChallenge activeChallenge;

    @BeforeEach
    void setUp() {
        userDailyChallengeRepository.deleteAll();
        dailyChallengeRepository.deleteAll();

        userA = new User();
        userA.setFullName("User A");
        userA.setPhoneNumber("0987654321");
        userA.setGender(Gender.MALE);
        userA.setStatus("ACTIVE");
        userA.setDateOfBirth(LocalDate.of(2001, 1, 1));
        userA = userRepository.save(userA);

        userB = new User();
        userB.setFullName("User B");
        userB.setPhoneNumber("0987654322");
        userB.setGender(Gender.FEMALE);
        userB.setStatus("ACTIVE");
        userB.setDateOfBirth(LocalDate.of(2002, 2, 2));
        userB = userRepository.save(userB);

        activeChallenge = DailyChallenge.builder()
                .title("Save $5 today")
                .description("Put $5 into one of your savings goals.")
                .challengeType(ChallengeType.TIP)
                .rule(ChallengeRule.builder().ruleType(ChallengeRuleType.MANUAL_CONFIRM).build())
                .xpReward(25)
                .isActive(true)
                .build();
        activeChallenge = dailyChallengeRepository.save(activeChallenge);
    }

    @Test
    void test1_UserHasNoChallengeToday_CreatesUserDailyChallenge() {
        Optional<UserDailyChallengeResponse> responseOpt = dailyChallengeService.getTodayChallenge(userA);
        assertTrue(responseOpt.isPresent());

        UserDailyChallengeResponse response = responseOpt.get();
        assertNotNull(response.getUserDailyChallengeId());
        assertEquals(activeChallenge.getId(), response.getChallengeId());
        assertEquals("Save $5 today", response.getTitle());
        assertEquals(ChallengeStatus.PENDING, response.getStatus());
        assertEquals(LocalDate.now(), response.getChallengeDate());
    }

    @Test
    void test2_UserAlreadyHasTodayChallenge_ReturnsExistingRecord() {
        Optional<UserDailyChallengeResponse> firstCall = dailyChallengeService.getTodayChallenge(userA);
        assertTrue(firstCall.isPresent());

        Optional<UserDailyChallengeResponse> secondCall = dailyChallengeService.getTodayChallenge(userA);
        assertTrue(secondCall.isPresent());

        assertEquals(firstCall.get().getUserDailyChallengeId(), secondCall.get().getUserDailyChallengeId());
    }

    @Test
    void test3_UserCallsEndpointMultipleTimes_OnlyOneRecordExists() {
        dailyChallengeService.getTodayChallenge(userA);
        dailyChallengeService.getTodayChallenge(userA);
        dailyChallengeService.getTodayChallenge(userA);

        boolean exists = userDailyChallengeRepository.existsByUserAndChallengeDate(userA, LocalDate.now());
        assertTrue(exists);

        assertEquals(1, userDailyChallengeRepository.findAll().stream()
                .filter(u -> u.getUser().getId().equals(userA.getId()) && u.getChallengeDate().equals(LocalDate.now()))
                .count());
    }

    @Test
    void test5_NoActiveChallenges_ReturnsEmpty() {
        dailyChallengeRepository.deleteAll();

        Optional<UserDailyChallengeResponse> responseOpt = dailyChallengeService.getTodayChallenge(userA);
        assertTrue(responseOpt.isEmpty(), "When no active challenges exist, service returns empty optional");
    }

    @Test
    void test6_UserA_CannotAccessOrComplete_UserB_DailyChallenge() {
        UserDailyChallengeResponse userBChallenge = dailyChallengeService.getTodayChallenge(userB).orElseThrow();

        assertThrows(AccessDeniedException.class, () -> {
            dailyChallengeService.completeChallenge(userBChallenge.getUserDailyChallengeId(), userA);
        });
    }

    @Test
    void test7_UserCompletesTodayChallenge_StatusBecomesCompletedAndXpAwarded() {
        UserDailyChallengeResponse challengeResponse = dailyChallengeService.getTodayChallenge(userA).orElseThrow();

        UserDailyChallengeResponse completedResponse = dailyChallengeService.completeChallenge(challengeResponse.getUserDailyChallengeId(), userA);

        assertEquals(ChallengeStatus.COMPLETED, completedResponse.getStatus());
        assertNotNull(completedResponse.getCompletedAt());

        UserReward reward = userRewardRepository.findByUser(userA).orElseThrow();
        assertEquals(25, reward.getTotalXp(), "XP reward should be added to user's total XP");
    }

    @Test
    void test8_UserCompletesSameChallengeTwice_XpNotAwardedTwice() {
        UserDailyChallengeResponse challengeResponse = dailyChallengeService.getTodayChallenge(userA).orElseThrow();

        dailyChallengeService.completeChallenge(challengeResponse.getUserDailyChallengeId(), userA);
        UserReward rewardAfterFirst = userRewardRepository.findByUser(userA).orElseThrow();
        int xpAfterFirst = rewardAfterFirst.getTotalXp();

        dailyChallengeService.completeChallenge(challengeResponse.getUserDailyChallengeId(), userA);
        UserReward rewardAfterSecond = userRewardRepository.findByUser(userA).orElseThrow();

        assertEquals(xpAfterFirst, rewardAfterSecond.getTotalXp(), "Completing challenge twice must NOT award XP twice");
    }
}
