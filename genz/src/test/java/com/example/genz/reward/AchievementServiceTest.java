package com.example.genz.reward;

import com.example.genz.reward.dto.AchievementResponse;
import com.example.genz.reward.dto.UserRewardSummaryResponse;
import com.example.genz.reward.dto.XpTransactionResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.entity.UserBadge;
import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.reward.repository.BadgeRepository;
import com.example.genz.reward.repository.UserBadgeRepository;
import com.example.genz.reward.repository.UserRewardRepository;
import com.example.genz.reward.service.AchievementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:genz_achieve_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
		"jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long"
})
@Transactional
class AchievementServiceTest {

	@Autowired
	private AchievementService achievementService;

	@Autowired
	private BadgeRepository badgeRepository;

	@Autowired
	private UserBadgeRepository userBadgeRepository;

	@Autowired
	private UserRewardRepository userRewardRepository;

	@Autowired
	private UserRepository userRepository;

	private User testUser;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setFullName("Achievement Tester");
		testUser.setPhoneNumber("0912345678");
		testUser.setGender(Gender.MALE);
		testUser.setStatus("ACTIVE");
		testUser.setDateOfBirth(LocalDate.of(2000, 1, 1));
		testUser = userRepository.save(testUser);
	}

	@Test
	void testFortyEightBadgesSeeded() {
		long totalBadges = badgeRepository.count();
		assertEquals(48, totalBadges, "System should contain exactly 48 seeded achievement badges");

		for (BadgeCategory category : BadgeCategory.values()) {
			List<Badge> categoryBadges = badgeRepository.findByCategoryAndActiveTrue(category);
			assertFalse(categoryBadges.isEmpty(), "Category " + category + " should have active seeded badges");
		}
	}

	@Test
	void testEvaluateAndUnlockAchievement() {
		// 1. Evaluate FIRST_GOAL_CREATED
		List<UserBadge> unlocked = achievementService.evaluateAndUnlockAchievements(
				testUser,
				BadgeRequirementType.FIRST_GOAL_CREATED,
				Map.of("createdGoalCount", 1)
		);

		assertEquals(1, unlocked.size());
		assertEquals("First Goal Created", unlocked.get(0).getBadge().getName());

		// 2. Check UserReward summary and User.totalXp updated
		UserRewardSummaryResponse summary = achievementService.getUserRewardSummary(testUser);
		assertEquals(1, summary.getAchievementCount());
		assertEquals(50, summary.getTotalXp());
		assertEquals(1, summary.getLevel());

		User refreshedUser = userRepository.findById(testUser.getId()).orElseThrow();
		assertEquals(50, refreshedUser.getTotalXp(), "User entity totalXp must be updated when achievement unlocks");

		// 3. Verify XP history recorded
		List<XpTransactionResponse> xpHistory = achievementService.getXpHistory(testUser);
		assertEquals(1, xpHistory.size());
		assertEquals(50, xpHistory.get(0).getPoints());

		// 4. Test idempotency (repeat unlock attempt does not duplicate badge or XP)
		List<UserBadge> secondAttempt = achievementService.evaluateAndUnlockAchievements(
				testUser,
				BadgeRequirementType.FIRST_GOAL_CREATED,
				Map.of("createdGoalCount", 1)
		);
		assertTrue(secondAttempt.isEmpty(), "Re-evaluating should not unlock duplicate badge");

		UserRewardSummaryResponse summaryAfterRepeat = achievementService.getUserRewardSummary(testUser);
		assertEquals(50, summaryAfterRepeat.getTotalXp(), "XP must not be awarded twice for same achievement");
	}

	@Test
	void testProgressCalculationForLockedAndUnlockedBadges() {
		// Evaluate 7-day streak requirement with progress = 4 days (not yet met)
		achievementService.evaluateAndUnlockAchievements(
				testUser,
				BadgeRequirementType.STREAK_DAYS,
				Map.of("streak", 4)
		);

		List<AchievementResponse> allAchievements = achievementService.getAllAchievementsForUser(testUser);
		assertFalse(allAchievements.isEmpty());

		AchievementResponse streak3Badge = allAchievements.stream()
				.filter(a -> "3-Day Streak".equals(a.getName()))
				.findFirst()
				.orElseThrow();

		// 3-Day Streak should be unlocked because 4 >= 3
		assertTrue(streak3Badge.getUnlocked());
		assertEquals(100, streak3Badge.getProgressPercentage());

		AchievementResponse streak7Badge = allAchievements.stream()
				.filter(a -> "7-Day Streak".equals(a.getName()))
				.findFirst()
				.orElseThrow();

		// 7-Day Streak should be locked, current progress = 4 out of 7 => 57%
		assertFalse(streak7Badge.getUnlocked());
		assertEquals(4, streak7Badge.getCurrentProgress());
		assertEquals(7, streak7Badge.getRequirementValue());
		assertEquals(57, streak7Badge.getProgressPercentage());
	}
}
