package com.example.genz.reward;

import com.example.genz.reward.enums.GoalMilestone;
import com.example.genz.reward.service.GoalMilestoneRewardTierService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoalMilestoneRewardTierServiceTest {

    private final GoalMilestoneRewardTierService tierService = new GoalMilestoneRewardTierService();

    @Test
    @DisplayName("Goals below $50 should award 0 coins")
    void testBelowFifty() {
        assertEquals(0, tierService.getRewardCoins(new BigDecimal("0"), GoalMilestone.MILESTONE_25));
        assertEquals(0, tierService.getRewardCoins(new BigDecimal("49.99"), GoalMilestone.MILESTONE_25));
        assertEquals(0, tierService.getRewardCoins(new BigDecimal("49.99"), GoalMilestone.MILESTONE_100));
        assertEquals(0, tierService.getRewardCoins(null, GoalMilestone.MILESTONE_100));
    }

    @ParameterizedTest(name = "Target {0}: 25%={1}, 50%={2}, 75%={3}, 100%={4}")
    @CsvSource({
            // Tier 1: $50–$199
            "50,     10,  20,  30,  75",
            "100,    10,  20,  30,  75",
            "199,    10,  20,  30,  75",
            "199.99, 10,  20,  30,  75",

            // Tier 2: $200–$499
            "200,    15,  30,  45, 100",
            "350,    15,  30,  45, 100",
            "499,    15,  30,  45, 100",
            "499.99, 15,  30,  45, 100",

            // Tier 3: $500–$999
            "500,    25,  50,  75, 150",
            "750,    25,  50,  75, 150",
            "999,    25,  50,  75, 150",
            "999.99, 25,  50,  75, 150",

            // Tier 4: $1,000–$1,999
            "1000,    40,  80, 120, 250",
            "1500,    40,  80, 120, 250",
            "1999,    40,  80, 120, 250",
            "1999.99, 40,  80, 120, 250",

            // Tier 5: $2,000–$4,999
            "2000,    60, 120, 180, 350",
            "3000,    60, 120, 180, 350",
            "4999,    60, 120, 180, 350",
            "4999.99, 60, 120, 180, 350",

            // Tier 6: $5,000–$9,999
            "5000,    80, 160, 240, 500",
            "7500,    80, 160, 240, 500",
            "9999,    80, 160, 240, 500",
            "9999.99, 80, 160, 240, 500",

            // Tier 7: $10,000–$14,999
            "10000,    100, 200, 300, 700",
            "12500,    100, 200, 300, 700",
            "14999,    100, 200, 300, 700",
            "14999.99, 100, 200, 300, 700",

            // Tier 8: $15,000–$19,999
            "15000,    125, 250, 375, 900",
            "17500,    125, 250, 375, 900",
            "19999,    125, 250, 375, 900",
            "19999.99, 125, 250, 375, 900",

            // Tier 9: $20,000+ (Max tier)
            "20000,    150, 300, 450, 1000",
            "50000,    150, 300, 450, 1000",
            "100000,   150, 300, 450, 1000",
            "1000000,  150, 300, 450, 1000"
    })
    @DisplayName("Verify every tier and boundary amount")
    void testEveryTierAndBoundary(String amountStr, int r25, int r50, int r75, int r100) {
        BigDecimal amount = new BigDecimal(amountStr);
        assertEquals(r25, tierService.getRewardCoins(amount, GoalMilestone.MILESTONE_25), "25% reward mismatch for " + amountStr);
        assertEquals(r50, tierService.getRewardCoins(amount, GoalMilestone.MILESTONE_50), "50% reward mismatch for " + amountStr);
        assertEquals(r75, tierService.getRewardCoins(amount, GoalMilestone.MILESTONE_75), "75% reward mismatch for " + amountStr);
        assertEquals(r100, tierService.getRewardCoins(amount, GoalMilestone.MILESTONE_100), "100% reward mismatch for " + amountStr);
    }
}
