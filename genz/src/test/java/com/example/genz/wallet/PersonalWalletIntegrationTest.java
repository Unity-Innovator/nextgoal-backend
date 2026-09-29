package com.example.genz.wallet;

import com.example.genz.auth.dto.RegisterRequest;
import com.example.genz.auth.dto.RegisterResponse;
import com.example.genz.auth.entity.RefreshToken;
import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.OtpPurpose;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.RefreshTokenRepository;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.auth.repository.VerificationTokenRepository;
import com.example.genz.auth.service.AuthService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.auth.util.TokenHashUtil;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.GoalType;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.transaction.dto.CreateWithdrawalRequest;
import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.dto.WithdrawalRequestResponse;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.GroupWalletTransaction;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.enums.*;
import com.example.genz.reward.repository.GoalMilestoneRewardRepository;
import com.example.genz.transaction.repository.GroupWalletTransactionRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.transaction.repository.WithdrawalApprovalRepository;
import com.example.genz.transaction.repository.WithdrawalRequestRepository;
import com.example.genz.transaction.service.WithdrawalRequestService;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.dto.TopUpRequest;
import com.example.genz.wallet.dto.WalletResponse;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.entity.Wallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.wallet.repository.WalletRepository;
import com.example.genz.wallet.service.WalletService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        "spring.datasource.url=jdbc:h2:mem:genz_wallet_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@AutoConfigureMockMvc
public class PersonalWalletIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private GoalContributionRepository goalContributionRepository;

    @Autowired
    private GroupWalletTransactionRepository groupWalletTransactionRepository;

    @Autowired
    private WithdrawalRequestService withdrawalRequestService;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Autowired
    private WithdrawalApprovalRepository withdrawalApprovalRepository;

    @Autowired
    private GoalMilestoneRewardRepository goalMilestoneRewardRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User alice;
    private User bob;
    private Goal vacationGoal;
    private GrouplWallet goalWallet;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
        if (suffix.length() < 8) {
            suffix = String.valueOf(System.currentTimeMillis());
        }
        suffix = suffix.substring(0, 8);
        String alicePhone = "+1" + suffix;
        String bobPhone = "+2" + suffix;

        alice = userRepository.save(User.builder()
                .fullName("Alice Wonderland")
                .phoneNumber(alicePhone)
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .coinBalance(0)
                .build());

        bob = userRepository.save(User.builder()
                .fullName("Bob Builder")
                .phoneNumber(bobPhone)
                .dateOfBirth(LocalDate.of(1998, 5, 12))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("654321"))
                .status("ACTIVE")
                .role(UserRole.USER)
                .coinBalance(0)
                .build());

        // Create Alice's personal wallet with initial balance 500.00
        walletRepository.save(Wallet.builder()
                .user(alice)
                .balance(new BigDecimal("500.00"))
                .currency(Currency.USD)
                .build());

        // Create Bob's personal wallet with initial balance 200.00
        walletRepository.save(Wallet.builder()
                .user(bob)
                .balance(new BigDecimal("200.00"))
                .currency(Currency.USD)
                .build());

        // Setup a goal owned by Alice with Bob as an active member
        vacationGoal = new Goal();
        vacationGoal.setGoalName("Hawaii Vacation");
        vacationGoal.setTargetAmount(new BigDecimal("1000.00"));
        vacationGoal.setCurrentAmount(BigDecimal.ZERO);
        vacationGoal.setRemaining(new BigDecimal("1000.00"));
        vacationGoal.setTargetDate(LocalDate.now().plusMonths(6));
        vacationGoal.setCategory(GoalCategory.TRAVEL);
        vacationGoal.setGoalType(GoalType.FLEXIBLE);
        vacationGoal.setStatus(GoalStatus.ACTIVE);
        vacationGoal.setOwner(alice);
        vacationGoal.setCreatedAt(LocalDateTime.now());
        vacationGoal = goalRepository.save(vacationGoal);

        goalWallet = new GrouplWallet();
        goalWallet.setGoal(vacationGoal);
        goalWallet.setUser(alice);
        goalWallet.setBalance(BigDecimal.ZERO);
        goalWallet.setCurrency(Currency.USD);
        goalWallet = goalWalletRepository.save(goalWallet);

        GoalMember aliceMember = new GoalMember();
        aliceMember.setGoal(vacationGoal);
        aliceMember.setUser(alice);
        aliceMember.setRole(MemberRole.OWNER);
        aliceMember.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(aliceMember);

        GoalMember bobMember = new GoalMember();
        bobMember.setGoal(vacationGoal);
        bobMember.setUser(bob);
        bobMember.setRole(MemberRole.MEMBER);
        bobMember.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(bobMember);
    }

    // =========================================================================
    // 1. WALLET CREATION & ACCESS TESTS
    // =========================================================================

    @Test
    @DisplayName("Create wallet for new user: Registration automatically creates personal wallet with 0.00 balance")
    void testCreateWalletForNewUserViaRegistration() {
        String newPhone = "+3" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8);
        String rawToken = "test-verify-token-12345";
        VerificationToken vt = VerificationToken.builder()
                .tokenHash(TokenHashUtil.sha256Hex(rawToken))
                .phoneNumber(newPhone)
                .purpose(OtpPurpose.REGISTRATION)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        verificationTokenRepository.save(vt);

        RegisterRequest req = new RegisterRequest();
        req.setPhoneNumber(newPhone);
        req.setFirstName("Charlie");
        req.setLastName("Brown");
        req.setPassword("Password123!");
        req.setConfirmPassword("Password123!");
        req.setPin("112233");
        req.setVerificationToken(rawToken);
        req.setDateOfBirth(LocalDate.of(2002, 3, 15));
        req.setGender(Gender.MALE);

        RegisterResponse response = authService.register(req);
        assertNotNull(response);

        User charlie = userRepository.findByPhoneNumber(newPhone).orElseThrow();
        Wallet charlieWallet = walletRepository.findByUserId(charlie.getId()).orElseThrow();
        assertNotNull(charlieWallet);
        assertEquals(0, BigDecimal.ZERO.compareTo(charlieWallet.getBalance()));
        assertEquals(Currency.USD, charlieWallet.getCurrency());
    }

    @Test
    @DisplayName("Existing user gets wallet: User without wallet automatically gets one created on first access")
    void testExistingUserGetsWalletLazily() {
        String legacyPhone = "+4" + UUID.randomUUID().toString().replaceAll("[^0-9]", "").substring(0, 8);
        User legacyUser = userRepository.save(User.builder()
                .fullName("Legacy Dave")
                .phoneNumber(legacyPhone)
                .dateOfBirth(LocalDate.of(1995, 1, 1))
                .gender(Gender.OTHER)
                .status("ACTIVE")
                .role(UserRole.USER)
                .build());

        // Verify Dave has no wallet in DB
        assertTrue(walletRepository.findByUserId(legacyUser.getId()).isEmpty());

        // Call wallet retrieval
        WalletResponse response = walletService.getWalletByPhoneNumber(legacyUser.getPhoneNumber());
        assertNotNull(response);
        assertNotNull(response.getWalletId());
        assertEquals(0, BigDecimal.ZERO.compareTo(response.getBalance()));
        assertEquals("USD", response.getCurrency());

        // Confirm DB now contains the wallet
        Wallet savedWallet = walletRepository.findByUserId(legacyUser.getId()).orElseThrow();
        assertEquals(response.getWalletId(), savedWallet.getId());
    }

    @Test
    @DisplayName("Get authenticated user's wallet via GET /api/wallet")
    void testGetAuthenticatedUserWalletEndpoint() throws Exception {
        String token = jwtUtil.generateAccessToken(alice.getPhoneNumber());

        mockMvc.perform(get("/api/wallet")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Wallet retrieved successfully"))
                .andExpect(jsonPath("$.data.balance").value(500.00))
                .andExpect(jsonPath("$.data.currency").value("USD"));
    }

    @Test
    @DisplayName("Top up wallet: Increases balance and creates TOP_UP transaction with IN direction")
    void testTopUpWallet() throws Exception {
        String token = jwtUtil.generateAccessToken(alice.getPhoneNumber());
        TopUpRequest req = new TopUpRequest(new BigDecimal("150.00"), "Deposit from bank");

        mockMvc.perform(post("/api/wallet/top-up")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(650.00));

        Wallet aliceWallet = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("650.00").compareTo(aliceWallet.getBalance()));

        List<WalletTransaction> txs = walletTransactionRepository.findByUser_Id(alice.getId(), null).getContent();
        assertEquals(1, txs.size());
        WalletTransaction tx = txs.get(0);
        assertEquals(TransactionType.TOP_UP, tx.getType());
        assertEquals(TransactionDirection.IN, tx.getDirection());
        assertEquals(0, new BigDecimal("150.00").compareTo(tx.getAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(tx.getBalanceBefore()));
        assertEquals(0, new BigDecimal("650.00").compareTo(tx.getBalanceAfter()));
        assertNull(tx.getGoal());
    }

    // =========================================================================
    // 2. GOAL DEPOSIT TESTS
    // =========================================================================

    @Test
    @DisplayName("Goal Deposit: Enough wallet balance -> wallet decreases, goal increases, transactions recorded")
    void testGoalDeposit_Success() {
        // Alice has 500.00 in wallet, deposits 100.00 into goal
        DepositRequest req = new DepositRequest(vacationGoal.getId(), new BigDecimal("100.00"));
        DepositResponse response = walletService.deposit(req, alice.getPhoneNumber());

        assertNotNull(response);
        assertTrue(response.isSuccess());
        // User available wallet balance is now 400.00
        assertEquals(0, new BigDecimal("400.00").compareTo(response.getBalance()));
        assertEquals(0, new BigDecimal("100.00").compareTo(response.getCurrentAmount()));

        // Check wallet in DB
        Wallet aliceWallet = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("400.00").compareTo(aliceWallet.getBalance()));

        // Check goal in DB
        Goal updatedGoal = goalRepository.findById(vacationGoal.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(updatedGoal.getCurrentAmount()));
        assertEquals(0, new BigDecimal("900.00").compareTo(updatedGoal.getRemaining()));

        // Check GrouplWallet in DB
        GrouplWallet updatedGoalWallet = goalWalletRepository.findByGoal(vacationGoal).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(updatedGoalWallet.getBalance()));

        // Check WalletTransaction recorded on personal wallet
        List<WalletTransaction> txs = walletTransactionRepository.findByUser_Id(alice.getId(), null).getContent();
        assertEquals(1, txs.size());
        WalletTransaction tx = txs.get(0);
        assertEquals(TransactionType.GOAL_DEPOSIT, tx.getType());
        assertEquals(TransactionDirection.OUT, tx.getDirection());
        assertEquals(0, new BigDecimal("100.00").compareTo(tx.getAmount()));
        assertEquals(0, new BigDecimal("500.00").compareTo(tx.getBalanceBefore()));
        assertEquals(0, new BigDecimal("400.00").compareTo(tx.getBalanceAfter()));
        assertEquals(vacationGoal.getId(), tx.getGoal().getId());
        assertEquals(alice.getId(), tx.getUser().getId());

        // Check GoalContribution created
        List<GoalContribution> contributions = goalContributionRepository.findByGoalIdOrderByContributedAtAsc(vacationGoal.getId());
        assertEquals(1, contributions.size());
        assertEquals(0, new BigDecimal("100.00").compareTo(contributions.get(0).getAmount()));
    }

    @Test
    @DisplayName("Goal Deposit: Insufficient wallet balance -> rejected, nothing modified")
    void testGoalDeposit_InsufficientBalance_Rejected() {
        // Alice has 500.00, tries to deposit 600.00
        DepositRequest req = new DepositRequest(vacationGoal.getId(), new BigDecimal("600.00"));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                walletService.deposit(req, alice.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("Insufficient wallet balance"));

        // Verify wallet was NOT modified
        Wallet aliceWallet = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("500.00").compareTo(aliceWallet.getBalance()));

        // Verify goal was NOT modified
        Goal updatedGoal = goalRepository.findById(vacationGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedGoal.getCurrentAmount()));

        // Verify goal wallet was NOT modified
        GrouplWallet updatedGoalWallet = goalWalletRepository.findByGoal(vacationGoal).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedGoalWallet.getBalance()));

        // Verify NO transactions or contributions were created for Alice / this goal
        assertEquals(0, walletTransactionRepository.findByUser_Id(alice.getId(), null).getTotalElements());
        assertEquals(0, goalContributionRepository.findByGoalIdOrderByContributedAtAsc(vacationGoal.getId()).size());
    }

    // =========================================================================
    // 3. GOAL WITHDRAWAL TESTS
    // =========================================================================

    @Test
    @DisplayName("Goal Withdrawal: Full lifecycle - Pending/Rejected leaves wallet unchanged, Confirmation credits requester wallet")
    void testGoalWithdrawal_CompleteWorkflow() {
        // First, Alice deposits 400.00 into goal (Goal has 400.00, Alice wallet has 100.00 left)
        walletService.deposit(new DepositRequest(vacationGoal.getId(), new BigDecimal("400.00")), alice.getPhoneNumber());

        Wallet aliceWalletBeforeWithdrawal = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(aliceWalletBeforeWithdrawal.getBalance()));

        // Step 1: Alice creates a withdrawal request for 250.00
        CreateWithdrawalRequest withdrawReq = CreateWithdrawalRequest.builder()
                .goalId(vacationGoal.getId())
                .amount(new BigDecimal("250.00"))
                .reason("Need funds for flight")
                .build();

        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(
                vacationGoal.getId(), withdrawReq, alice.getPhoneNumber()
        );
        assertEquals(WithdrawalStatus.PENDING, created.getStatus());

        // CRITICAL CHECK: While PENDING, requester wallet MUST NOT be changed!
        Wallet walletWhilePending = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(walletWhilePending.getBalance()));

        // Step 2: Bob (the other member) approves the request
        WithdrawalRequestResponse approved = withdrawalRequestService.approveRequest(created.getId(), bob.getPhoneNumber());
        assertEquals(WithdrawalStatus.APPROVED, approved.getStatus());

        // CRITICAL CHECK: While APPROVED but not yet confirmed, wallet still MUST NOT be changed!
        Wallet walletWhileApproved = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(walletWhileApproved.getBalance()));

        // Step 3: Alice confirms withdrawal
        WithdrawalRequestResponse confirmed = withdrawalRequestService.confirmWithdrawal(created.getId(), alice.getPhoneNumber());
        assertEquals(WithdrawalStatus.COMPLETED, confirmed.getStatus());

        // 1. Goal amount decreased from 400.00 to 150.00
        Goal reloadedGoal = goalRepository.findById(vacationGoal.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("150.00").compareTo(reloadedGoal.getCurrentAmount()));

        // 2. Goal wallet balance decreased from 400.00 to 150.00
        GrouplWallet reloadedGoalWallet = goalWalletRepository.findByGoal(vacationGoal).orElseThrow();
        assertEquals(0, new BigDecimal("150.00").compareTo(reloadedGoalWallet.getBalance()));

        // 3. Requester's personal wallet increased from 100.00 to 350.00!
        Wallet aliceWalletAfter = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("350.00").compareTo(aliceWalletAfter.getBalance()));

        // 4. WalletTransaction created for requester
        List<WalletTransaction> walletTxs = walletTransactionRepository.findByUser_Id(alice.getId(), null).getContent();
        // 1 from deposit + 1 from withdrawal = 2
        assertEquals(2, walletTxs.size());
        WalletTransaction withdrawTx = walletTxs.stream()
                .filter(t -> t.getType() == TransactionType.GOAL_WITHDRAWAL)
                .findFirst()
                .orElseThrow();
        assertEquals(TransactionDirection.IN, withdrawTx.getDirection());
        assertEquals(0, new BigDecimal("250.00").compareTo(withdrawTx.getAmount()));
        assertEquals(0, new BigDecimal("100.00").compareTo(withdrawTx.getBalanceBefore()));
        assertEquals(0, new BigDecimal("350.00").compareTo(withdrawTx.getBalanceAfter()));
        assertEquals(vacationGoal.getId(), withdrawTx.getGoal().getId());
        assertEquals(alice.getId(), withdrawTx.getUser().getId());

        // 5. GroupWalletTransaction recorded for goal
        List<GroupWalletTransaction> groupTxs = groupWalletTransactionRepository.findByGroupWalletIdOrderByCreatedAtDesc(goalWallet.getId());
        assertEquals(1, groupTxs.size());
        assertEquals(GroupTransactionType.WITHDRAWAL, groupTxs.get(0).getTransactionType());
        assertEquals(0, new BigDecimal("250.00").compareTo(groupTxs.get(0).getAmount()));
    }

    @Test
    @DisplayName("Goal Withdrawal Rejected: Wallet remains unchanged if member rejects")
    void testGoalWithdrawal_RejectedLeavesWalletUnchanged() {
        // Alice deposits 300.00
        walletService.deposit(new DepositRequest(vacationGoal.getId(), new BigDecimal("300.00")), alice.getPhoneNumber());

        CreateWithdrawalRequest withdrawReq = CreateWithdrawalRequest.builder()
                .goalId(vacationGoal.getId())
                .amount(new BigDecimal("100.00"))
                .reason("Test reject")
                .build();

        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(
                vacationGoal.getId(), withdrawReq, alice.getPhoneNumber()
        );

        // Bob rejects
        WithdrawalRequestResponse rejected = withdrawalRequestService.rejectRequest(created.getId(), bob.getPhoneNumber());
        assertEquals(WithdrawalStatus.REJECTED, rejected.getStatus());

        // Goal and wallet are untouched
        Goal reloadedGoal = goalRepository.findById(vacationGoal.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("300.00").compareTo(reloadedGoal.getCurrentAmount()));

        Wallet aliceWallet = walletRepository.findByUserId(alice.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("200.00").compareTo(aliceWallet.getBalance()));
    }

    // =========================================================================
    // 4. CONCURRENCY / DOUBLE SPENDING TEST
    // =========================================================================

    @Test
    @DisplayName("Concurrency: Concurrent deposits exceeding wallet balance cannot double-spend")
    void testConcurrentGoalDepositsDoubleSpendPrevented() throws Exception {
        // Set Bob's wallet balance to exactly 100.00
        Wallet bobWallet = walletRepository.findByUserId(bob.getId()).orElseThrow();
        bobWallet.setBalance(new BigDecimal("100.00"));
        walletRepository.saveAndFlush(bobWallet);

        int numberOfThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CyclicBarrier barrier = new CyclicBarrier(numberOfThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executor.submit(() -> {
                try {
                    barrier.await();
                    // Each thread attempts to deposit 80.00 from Bob's 100.00 wallet
                    DepositRequest req = new DepositRequest(vacationGoal.getId(), new BigDecimal("80.00"));
                    walletService.deposit(req, bob.getPhoneNumber());
                    successCount.incrementAndGet();
                } catch (BadRequestException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one request must succeed, and one must fail!
        assertEquals(1, successCount.get(), "Only 1 deposit of 80.00 from a 100.00 wallet must succeed");
        assertEquals(1, failureCount.get(), "1 deposit must be rejected due to insufficient balance");

        // Final wallet balance MUST be exactly 20.00 (100 - 80)
        Wallet reloadedBobWallet = walletRepository.findByUserId(bob.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("20.00").compareTo(reloadedBobWallet.getBalance()));

        // Final goal balance MUST be exactly 80.00
        Goal reloadedGoal = goalRepository.findById(vacationGoal.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("80.00").compareTo(reloadedGoal.getCurrentAmount()));
    }

    // =========================================================================
    // 5. AUTHORIZATION & ISOLATION TESTS
    // =========================================================================

    @Test
    @DisplayName("Authorization: User A cannot see User B's transactions via GET /api/wallet/transactions")
    void testWalletTransactionsAuthorizationIsolation() throws Exception {
        // Alice deposits 50.00
        walletService.deposit(new DepositRequest(vacationGoal.getId(), new BigDecimal("50.00")), alice.getPhoneNumber());

        // Bob deposits 30.00
        walletService.deposit(new DepositRequest(vacationGoal.getId(), new BigDecimal("30.00")), bob.getPhoneNumber());

        String aliceToken = jwtUtil.generateAccessToken(alice.getPhoneNumber());
        String bobToken = jwtUtil.generateAccessToken(bob.getPhoneNumber());

        // Alice sees only Alice's transactions
        mockMvc.perform(get("/api/wallet/transactions")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].amount").value(50.0))
                .andExpect(jsonPath("$.data.items[0].type").value("GOAL_DEPOSIT"))
                .andExpect(jsonPath("$.data.items[0].direction").value("OUT"))
                .andExpect(jsonPath("$.data.items[0].goalName").value("Hawaii Vacation"));

        // Bob sees only Bob's transactions
        mockMvc.perform(get("/api/wallet/transactions")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].amount").value(30.0))
                .andExpect(jsonPath("$.data.items[0].type").value("GOAL_DEPOSIT"))
                .andExpect(jsonPath("$.data.items[0].direction").value("OUT"))
                .andExpect(jsonPath("$.data.items[0].goalName").value("Hawaii Vacation"));
    }

    @Test
    @DisplayName("Top up wallet with bank info: Sets title to Top Up — ABA and returns reference ID")
    void testTopUpWalletWithBankAndPaymentMethod() throws Exception {
        String token = jwtUtil.generateAccessToken(alice.getPhoneNumber());
        TopUpRequest req = TopUpRequest.builder()
                .bank("ABA")
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .paymentMethod("BANK_TRANSFER")
                .build();

        mockMvc.perform(post("/api/wallet/top-up")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.newBalance").value(1000.00))
                .andExpect(jsonPath("$.data.bank").value("ABA"))
                .andExpect(jsonPath("$.data.paymentMethod").value("BANK_TRANSFER"))
                .andExpect(jsonPath("$.data.referenceId").isNotEmpty());

        List<WalletTransaction> txs = walletTransactionRepository.findByUser_Id(alice.getId(), null).getContent();
        assertEquals(1, txs.size());
        assertEquals("Top Up — ABA", txs.get(0).getDescription());
    }

    @Test
    @DisplayName("Get top up options: Returns supported banks, currencies, quick amounts, payment methods")
    void testGetTopUpOptionsEndpoint() throws Exception {
        String token = jwtUtil.generateAccessToken(alice.getPhoneNumber());

        mockMvc.perform(get("/api/wallet/top-up/options")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.banks.length()").value(3))
                .andExpect(jsonPath("$.data.banks[0].code").value("ABA"))
                .andExpect(jsonPath("$.data.currencies").isArray())
                .andExpect(jsonPath("$.data.quickAmounts").isArray())
                .andExpect(jsonPath("$.data.paymentMethods.length()").value(3));
    }

    @Test
    @DisplayName("Get wallet summary: Returns balance, monthly change, and recent transactions for mobile screen")
    void testGetWalletSummaryEndpoint() throws Exception {
        String token = jwtUtil.generateAccessToken(alice.getPhoneNumber());

        // Perform top up with ABA
        TopUpRequest topUpReq = TopUpRequest.builder()
                .bank("ABA")
                .amount(new BigDecimal("500.00"))
                .currency("USD")
                .paymentMethod("BANK_TRANSFER")
                .build();
        walletService.topUp(topUpReq, alice.getPhoneNumber());

        mockMvc.perform(get("/api/wallet/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.balance").value(1000.00))
                .andExpect(jsonPath("$.data.monthChangeAmount").value(500.0))
                .andExpect(jsonPath("$.data.recentTransactions.length()").value(1))
                .andExpect(jsonPath("$.data.recentTransactions[0].description").value("Top Up — ABA"))
                .andExpect(jsonPath("$.data.recentTransactions[0].type").value("TOP_UP"))
                .andExpect(jsonPath("$.data.recentTransactions[0].direction").value("IN"));
    }
}
