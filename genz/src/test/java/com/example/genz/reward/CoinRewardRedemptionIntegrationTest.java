package com.example.genz.reward;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.reward.dto.RedeemRewardRequest;
import com.example.genz.reward.dto.RedemptionHistoryResponse;
import com.example.genz.reward.dto.RedemptionResponse;
import com.example.genz.reward.entity.CoinReward;
import com.example.genz.reward.entity.CoinRewardSize;
import com.example.genz.reward.entity.RewardRedemption;
import com.example.genz.reward.enums.RewardRedemptionStatus;
import com.example.genz.reward.enums.RewardSize;
import com.example.genz.reward.repository.CoinRewardRepository;
import com.example.genz.reward.repository.CoinRewardSizeRepository;
import com.example.genz.reward.repository.RewardRedemptionRepository;
import com.example.genz.reward.service.CoinRewardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_reward_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@AutoConfigureMockMvc
class CoinRewardRedemptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CoinRewardService coinRewardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoinRewardRepository coinRewardRepository;

    @Autowired
    private CoinRewardSizeRepository coinRewardSizeRepository;

    @Autowired
    private RewardRedemptionRepository rewardRedemptionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private User alice;
    private User bob;
    private CoinReward tShirt;
    private CoinRewardSize sizeS;
    private CoinReward cap;

    @BeforeEach
    void setUp() {
        rewardRedemptionRepository.deleteAll();
        coinRewardSizeRepository.deleteAll();
        coinRewardRepository.deleteAll();
        userRepository.deleteAll();

        alice = new User();
        alice.setFullName("Alice");
        alice.setPhoneNumber("098111222");
        alice.setDateOfBirth(LocalDate.of(1998, 5, 10));
        alice.setGender(Gender.FEMALE);
        alice.setStatus("ACTIVE");
        alice.setRole(UserRole.USER);
        alice.setCoinBalance(2000);
        alice = userRepository.saveAndFlush(alice);

        bob = new User();
        bob.setFullName("Bob");
        bob.setPhoneNumber("098333444");
        bob.setDateOfBirth(LocalDate.of(1999, 6, 12));
        bob.setGender(Gender.MALE);
        bob.setStatus("ACTIVE");
        bob.setRole(UserRole.USER);
        bob.setCoinBalance(2000);
        bob = userRepository.saveAndFlush(bob);

        tShirt = CoinReward.builder()
                .name("NextGoal T-Shirt")
                .imageUrl("https://example.com/tshirt.png")
                .description("Exclusive branded tee")
                .redeemCoins(500)
                .stock(0)
                .isActive(true)
                .build();
        tShirt = coinRewardRepository.saveAndFlush(tShirt);

        sizeS = CoinRewardSize.builder()
                .coinReward(tShirt)
                .size(RewardSize.S)
                .stock(1) // only 1 unit in stock for concurrency testing
                .build();
        sizeS = coinRewardSizeRepository.saveAndFlush(sizeS);

        cap = CoinReward.builder()
                .name("NextGoal Cap")
                .imageUrl("https://example.com/cap.png")
                .description("Stylish branded cap")
                .redeemCoins(250)
                .stock(1) // only 1 unit for concurrency testing
                .isActive(true)
                .build();
        cap = coinRewardRepository.saveAndFlush(cap);
    }

    @Test
    @DisplayName("End-to-End API: Redeem sized reward via HTTP endpoint")
    @WithMockUser(username = "098111222")
    void testRedeemSizedRewardApi() throws Exception {
        RedeemRewardRequest request = RedeemRewardRequest.builder()
                .rewardId(tShirt.getId())
                .sizeId(sizeS.getId())
                .phoneNumber("098111222")
                .address("123 Monivong Blvd, Phnom Penh")
                .build();

        mockMvc.perform(post("/api/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.item").value("NextGoal T-Shirt"))
                .andExpect(jsonPath("$.data.size").value("S"))
                .andExpect(jsonPath("$.data.coinsUsed").value(500))
                .andExpect(jsonPath("$.data.remainingCoins").value(1500))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.referenceNumber").exists());

        // Check DB state
        User updatedAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertEquals(1500, updatedAlice.getCoinBalance());

        CoinRewardSize updatedSize = coinRewardSizeRepository.findById(sizeS.getId()).orElseThrow();
        assertEquals(0, updatedSize.getStock());

        List<RewardRedemption> redemptions = rewardRedemptionRepository.findByUserOrderByCreatedAtDesc(alice);
        assertEquals(1, redemptions.size());
        assertEquals("123 Monivong Blvd, Phnom Penh", redemptions.get(0).getDeliveryAddress());
    }

    @Test
    @DisplayName("End-to-End API: User only sees their own redemption history")
    @WithMockUser(username = "098111222")
    void testUserOnlySeesTheirOwnHistory() throws Exception {
        // Alice redeems cap
        RedeemRewardRequest reqAlice = RedeemRewardRequest.builder()
                .rewardId(cap.getId())
                .phoneNumber("098111222")
                .address("Alice Address")
                .build();
        coinRewardService.redeemReward(reqAlice, alice.getPhoneNumber());

        // Call history endpoint as Alice
        mockMvc.perform(get("/api/rewards/redemptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].item").value("NextGoal Cap"));

        // Bob queries his own history
        List<RedemptionHistoryResponse> bobHistory = coinRewardService.getRedemptionHistory(bob.getPhoneNumber());
        assertTrue(bobHistory.isEmpty(), "Bob should not see Alice's redemptions");
    }

    @Test
    @DisplayName("Validation: Phone number and address are required")
    @WithMockUser(username = "098111222")
    void testValidationRequiredFields() throws Exception {
        RedeemRewardRequest invalidRequest = RedeemRewardRequest.builder()
                .rewardId(cap.getId())
                .phoneNumber("") // missing!
                .address("")     // missing!
                .build();

        mockMvc.perform(post("/api/rewards/redeem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Concurrency: Two users cannot redeem the same final unit of stock simultaneously")
    void testConcurrentRedemptionOfFinalStock() throws Exception {
        // Cap has stock = 1
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Void> taskAlice = () -> {
            barrier.await();
            try {
                RedeemRewardRequest req = RedeemRewardRequest.builder()
                        .rewardId(cap.getId())
                        .phoneNumber("098111222")
                        .address("Alice Street")
                        .build();
                coinRewardService.redeemReward(req, alice.getPhoneNumber());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskBob = () -> {
            barrier.await();
            try {
                RedeemRewardRequest req = RedeemRewardRequest.builder()
                        .rewardId(cap.getId())
                        .phoneNumber("098333444")
                        .address("Bob Street")
                        .build();
                coinRewardService.redeemReward(req, bob.getPhoneNumber());
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(taskAlice);
        Future<Void> f2 = executor.submit(taskBob);

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one request must succeed and the other must fail (no overselling)
        assertEquals(1, successCount.get(), "Exactly one user should succeed in claiming the last unit");
        assertEquals(1, failureCount.get(), "The competing user should fail with out of stock");

        CoinReward finalCap = coinRewardRepository.findById(cap.getId()).orElseThrow();
        assertEquals(0, finalCap.getStock(), "Stock should be 0, never negative");

        List<RewardRedemption> redemptions = rewardRedemptionRepository.findAll();
        assertEquals(1, redemptions.size(), "Only one redemption record should be created");
    }

    @Test
    @DisplayName("Transaction Rollback: If transaction fails, coin deduction and stock reduction are rolled back")
    void testTransactionRollbackIntegrity() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            RedeemRewardRequest req = RedeemRewardRequest.builder()
                    .rewardId(cap.getId())
                    .phoneNumber("098111222")
                    .address("Alice Address")
                    .build();
            coinRewardService.redeemReward(req, alice.getPhoneNumber());

            // Force an intentional rollback
            transactionManager.rollback(status);
        } catch (Exception e) {
            if (!status.isCompleted()) {
                transactionManager.rollback(status);
            }
        }

        // Verify DB did NOT persist the redemption, coin deduction, or stock decrement
        List<RewardRedemption> redemptions = rewardRedemptionRepository.findAll();
        assertTrue(redemptions.isEmpty(), "Redemption should not be saved after transaction rollback");

        User freshAlice = userRepository.findById(alice.getId()).orElseThrow();
        assertEquals(2000, freshAlice.getCoinBalance(), "User coins should not be deducted after rollback");

        CoinReward freshCap = coinRewardRepository.findById(cap.getId()).orElseThrow();
        assertEquals(1, freshCap.getStock(), "Stock should not be decremented after rollback");
    }
}
