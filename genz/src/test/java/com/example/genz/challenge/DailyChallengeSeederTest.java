package com.example.genz.challenge;

import com.example.genz.challenge.config.DailyChallengeSeeder;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import com.example.genz.challenge.repository.DailyChallengeRepository;
import com.example.genz.challenge.repository.UserDailyChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_daily_seeder_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class DailyChallengeSeederTest {

    @Autowired
    private DailyChallengeRepository dailyChallengeRepository;

    @Autowired
    private UserDailyChallengeRepository userDailyChallengeRepository;

    @Autowired
    private DailyChallengeSeeder dailyChallengeSeeder;

    @BeforeEach
    void setUp() {
        userDailyChallengeRepository.deleteAll();
        dailyChallengeRepository.deleteAll();
    }

    @Test
    @DisplayName("Should seed 20 diverse daily challenges into empty database")
    void seedDailyChallenges_WhenEmpty_SeedsSuccessfully() throws Exception {
        assertEquals(0, dailyChallengeRepository.count());

        CommandLineRunner runner = dailyChallengeSeeder.seedDailyChallenges();
        runner.run();

        long count = dailyChallengeRepository.count();
        assertEquals(20, count, "Should seed exactly 20 daily challenges");

        List<DailyChallenge> all = dailyChallengeRepository.findAll();
        assertTrue(all.stream().allMatch(DailyChallenge::getIsActive), "All seeded challenges should be active");
        assertTrue(all.stream().allMatch(c -> c.getXpReward() != null && c.getXpReward() > 0), "All seeded challenges should have positive XP reward");

        boolean hasQuiz = all.stream().anyMatch(c -> c.getChallengeType() == ChallengeType.QUIZ);
        boolean hasExpenseLog = all.stream().anyMatch(c -> c.getChallengeType() == ChallengeType.EXPENSE_LOG);
        boolean hasTip = all.stream().anyMatch(c -> c.getChallengeType() == ChallengeType.TIP);

        assertTrue(hasQuiz, "Should contain QUIZ challenges");
        assertTrue(hasExpenseLog, "Should contain EXPENSE_LOG challenges");
        assertTrue(hasTip, "Should contain TIP challenges");
    }

    @Test
    @DisplayName("Should be idempotent and not duplicate data if challenges already exist")
    void seedDailyChallenges_WhenNotEmpty_DoesNotDuplicate() throws Exception {
        CommandLineRunner runner = dailyChallengeSeeder.seedDailyChallenges();
        runner.run();
        long initialCount = dailyChallengeRepository.count();
        assertEquals(20, initialCount);

        // Run second time
        runner.run();
        assertEquals(initialCount, dailyChallengeRepository.count(), "Count should remain unchanged after second run");
    }
}
