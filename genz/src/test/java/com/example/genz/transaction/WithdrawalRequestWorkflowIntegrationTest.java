package com.example.genz.transaction;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.security.jwt.JwtUtil;
import com.example.genz.transaction.dto.CreateWithdrawalRequest;
import com.example.genz.transaction.dto.WithdrawalRequestResponse;
import com.example.genz.transaction.entity.GroupWalletTransaction;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.entity.WithdrawalRequest;
import com.example.genz.transaction.enums.ApprovalDecision;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.GroupTransactionType;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.transaction.enums.WithdrawalStatus;
import com.example.genz.transaction.repository.GroupWalletTransactionRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.transaction.repository.WithdrawalApprovalRepository;
import com.example.genz.transaction.repository.WithdrawalRequestRepository;
import com.example.genz.transaction.service.WithdrawalRequestService;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_withdraw_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@Transactional
class WithdrawalRequestWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WithdrawalRequestService withdrawalRequestService;

    @Autowired
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Autowired
    private WithdrawalApprovalRepository withdrawalApprovalRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private GroupWalletTransactionRepository groupWalletTransactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User memberA; // Requester
    private User memberB; // Approver 1
    private User memberC; // Approver 2
    private User nonMember;

    private Goal sharedGoal;
    private GrouplWallet wallet;

    @BeforeEach
    void setUp() {
        memberA = userRepository.saveAndFlush(User.builder()
                .fullName("Sarah Johnson")
                .phoneNumber("+85512000001")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.FEMALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        memberB = userRepository.saveAndFlush(User.builder()
                .fullName("Alex Morgan")
                .phoneNumber("+85512000002")
                .dateOfBirth(LocalDate.of(1999, 2, 2))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        memberC = userRepository.saveAndFlush(User.builder()
                .fullName("Michael Chen")
                .phoneNumber("+85512000003")
                .dateOfBirth(LocalDate.of(2001, 3, 3))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        nonMember = userRepository.saveAndFlush(User.builder()
                .fullName("Stranger Danger")
                .phoneNumber("+85512000004")
                .dateOfBirth(LocalDate.of(1998, 4, 4))
                .gender(Gender.MALE)
                .passwordHash(passwordEncoder.encode("Password123!"))
                .pinHash(passwordEncoder.encode("123456"))
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        // Create shared goal with target $5,000 and currentAmount $2,000
        sharedGoal = new Goal();
        sharedGoal.setGoalName("Family Vacation Fund");
        sharedGoal.setTargetAmount(BigDecimal.valueOf(5000));
        sharedGoal.setCurrentAmount(BigDecimal.valueOf(2000));
        sharedGoal.setRemaining(BigDecimal.valueOf(3000));
        sharedGoal.setTargetDate(LocalDate.now().plusMonths(6));
        sharedGoal.setCategory(GoalCategory.TRAVEL);
        sharedGoal.setOwner(memberA);
        sharedGoal.setStatus(GoalStatus.ACTIVE);
        sharedGoal.setCreatedAt(LocalDateTime.now());
        sharedGoal = goalRepository.saveAndFlush(sharedGoal);

        // Add member A (OWNER)
        GoalMember gmA = new GoalMember();
        gmA.setGoal(sharedGoal);
        gmA.setUser(memberA);
        gmA.setRole(MemberRole.OWNER);
        gmA.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(gmA);

        // Add member B (MEMBER)
        GoalMember gmB = new GoalMember();
        gmB.setGoal(sharedGoal);
        gmB.setUser(memberB);
        gmB.setRole(MemberRole.MEMBER);
        gmB.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(gmB);

        // Add member C (MEMBER)
        GoalMember gmC = new GoalMember();
        gmC.setGoal(sharedGoal);
        gmC.setUser(memberC);
        gmC.setRole(MemberRole.MEMBER);
        gmC.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.saveAndFlush(gmC);

        // Goal wallet
        wallet = new GrouplWallet();
        wallet.setUser(memberA);
        wallet.setGoal(sharedGoal);
        wallet.setBalance(BigDecimal.valueOf(2000));
        wallet.setCurrency(Currency.USD);
        wallet = goalWalletRepository.saveAndFlush(wallet);
    }

    @Test
    @DisplayName("Create request: Member creates request -> PENDING -> goal balance unchanged")
    void createRequest_Success_PendingAndBalanceUnchanged() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Booking flights & hotel deposit")
                .build();

        WithdrawalRequestResponse res = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        assertNotNull(res);
        assertNotNull(res.getId());
        assertEquals(WithdrawalStatus.PENDING, res.getStatus());
        assertEquals(BigDecimal.valueOf(500), res.getAmount());
        assertEquals("Sarah Johnson", res.getRequesterName());
        assertEquals(2, res.getRequiredApprovals()); // Member B and Member C
        assertEquals(0, res.getApprovedCount());
        assertEquals(2, res.getPendingCount());

        // Verify goal balance is UNCHANGED
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(reloadedGoal.getCurrentAmount()));

        // Verify wallet balance is UNCHANGED
        GrouplWallet reloadedWallet = goalWalletRepository.findByGoal(sharedGoal).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(reloadedWallet.getBalance()));

        // Verify no wallet transactions created
        assertEquals(0, walletTransactionRepository.findAll().size());
    }

    @Test
    @DisplayName("Create request: Non-member cannot create withdrawal request")
    void createRequest_NonMember_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(300))
                .reason("Fraud attempt")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                withdrawalRequestService.createRequest(sharedGoal.getId(), req, nonMember.getPhoneNumber())
        );
    }

    @Test
    @DisplayName("Create request: Amount exceeding current goal balance is rejected")
    void createRequest_ExceedsBalance_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(2500)) // Current balance is 2000
                .reason("Overdraw")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("exceed current goal balance"));
    }

    @Test
    @DisplayName("Create request: Conflicting active withdrawal request is rejected")
    void createRequest_ConflictingActiveRequest_Rejected() {
        CreateWithdrawalRequest req1 = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(300))
                .reason("First request")
                .build();
        withdrawalRequestService.createRequest(sharedGoal.getId(), req1, memberA.getPhoneNumber());

        CreateWithdrawalRequest req2 = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(200))
                .reason("Second request")
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.createRequest(sharedGoal.getId(), req2, memberB.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("active withdrawal request already exists"));
    }

    @Test
    @DisplayName("Approve request: Eligible member approves -> approval APPROVED -> request remains PENDING until all required approvals")
    void approveRequest_SingleApproval_RemainsPending() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Vacation deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        // Member B approves
        WithdrawalRequestResponse approvedRes = withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());

        assertEquals(WithdrawalStatus.PENDING, approvedRes.getStatus());
        assertEquals(1, approvedRes.getApprovedCount());
        assertEquals(1, approvedRes.getPendingCount());

        // Balance remains unchanged
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(reloadedGoal.getCurrentAmount()));
    }

    @Test
    @DisplayName("Approve request: Requester cannot approve their own request")
    void approveRequest_ByRequester_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.approveRequest(created.getId(), memberA.getPhoneNumber())
        );
    }

    @Test
    @DisplayName("Approve request: Duplicate decision by same approver is rejected")
    void approveRequest_DuplicateDecision_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());

        assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber())
        );
    }

    @Test
    @DisplayName("Reject request: Member rejects -> request REJECTED -> no balance change")
    void rejectRequest_SingleMemberRejection_BecomesRejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        // Member B rejects
        WithdrawalRequestResponse rejectedRes = withdrawalRequestService.rejectRequest(created.getId(), memberB.getPhoneNumber());

        assertEquals(WithdrawalStatus.REJECTED, rejectedRes.getStatus());
        assertEquals(1, rejectedRes.getRejectedCount());

        // Goal balance unchanged
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(reloadedGoal.getCurrentAmount()));
    }

    @Test
    @DisplayName("All approvals: All required members approve -> request APPROVED -> balance unchanged -> requester can confirm")
    void allApprovals_BecomesApproved_BalanceUnchanged() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        // Member B approves
        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());

        // Member C approves
        WithdrawalRequestResponse finalApproval = withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        assertEquals(WithdrawalStatus.APPROVED, finalApproval.getStatus());
        assertEquals(2, finalApproval.getApprovedCount());
        assertEquals(0, finalApproval.getPendingCount());

        // Balance is still unchanged before confirmation
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(reloadedGoal.getCurrentAmount()));
    }

    @Test
    @DisplayName("Final confirmation: Requester confirms -> goal balance decreases -> goal remains -> transaction created -> COMPLETED")
    void finalConfirmation_Success_DecreasesBalance_GoalRemains_TransactionCreated() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        // Requester confirms
        WithdrawalRequestResponse confirmed = withdrawalRequestService.confirmWithdrawal(created.getId(), memberA.getPhoneNumber());

        assertEquals(WithdrawalStatus.COMPLETED, confirmed.getStatus());

        // CRITICAL CHECK: Goal MUST STILL EXIST in database!
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertNotNull(reloadedGoal);
        assertEquals(sharedGoal.getId(), reloadedGoal.getId());
        assertEquals(GoalStatus.ACTIVE, reloadedGoal.getStatus());

        // Goal currentAmount decreased from 2000 to 1500
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(reloadedGoal.getCurrentAmount()));
        // Remaining amount adjusted
        assertEquals(0, BigDecimal.valueOf(3500).compareTo(reloadedGoal.getRemaining()));

        // Wallet balance decreased to 1500
        GrouplWallet reloadedWallet = goalWalletRepository.findByGoal(sharedGoal).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(reloadedWallet.getBalance()));

        // WITHDRAWAL WalletTransaction created
        List<WalletTransaction> transactions = walletTransactionRepository.findAll();
        assertEquals(1, transactions.size());
        WalletTransaction tx = transactions.get(0);
        assertEquals(TransactionType.WITHDRAWAL, tx.getType());
        assertEquals(memberA.getId(), tx.getUser().getId());
        assertEquals(0, BigDecimal.valueOf(500).compareTo(tx.getAmount()));

        // GroupWalletTransaction created
        List<GroupWalletTransaction> groupTxs = groupWalletTransactionRepository.findAll();
        assertEquals(1, groupTxs.size());
        assertEquals(GroupTransactionType.WITHDRAWAL, groupTxs.get(0).getTransactionType());
        assertEquals(created.getId(), groupTxs.get(0).getWithdrawalRequest().getId());
    }

    @Test
    @DisplayName("Double confirmation: Second confirmation attempt is rejected")
    void finalConfirmation_DoubleConfirmation_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        // First confirmation succeeds
        withdrawalRequestService.confirmWithdrawal(created.getId(), memberA.getPhoneNumber());

        // Second confirmation attempt throws BadRequestException
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.confirmWithdrawal(created.getId(), memberA.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("already been completed"));

        // Verify balance was NOT decreased a second time (should remain 1500, not 1000)
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(1500).compareTo(reloadedGoal.getCurrentAmount()));

        // Only 1 transaction
        assertEquals(1, walletTransactionRepository.findAll().size());
    }

    @Test
    @DisplayName("Unauthorized confirmation: Non-requester cannot confirm")
    void finalConfirmation_NonRequester_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        // Member B attempts to confirm Member A's request
        assertThrows(AccessDeniedException.class, () ->
                withdrawalRequestService.confirmWithdrawal(created.getId(), memberB.getPhoneNumber())
        );
    }

    @Test
    @DisplayName("Rejected request: Cannot be confirmed")
    void finalConfirmation_RejectedRequest_CannotBeConfirmed() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        // Member B rejects
        withdrawalRequestService.rejectRequest(created.getId(), memberB.getPhoneNumber());

        assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.confirmWithdrawal(created.getId(), memberA.getPhoneNumber())
        );
    }

    @Test
    @DisplayName("Revalidate balance at confirmation: If balance was depleted, confirmation is rejected")
    void finalConfirmation_InsufficientBalanceAtConfirmation_Rejected() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(1500))
                .reason("Big withdrawal")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        // Meanwhile, goal balance is reduced to 1000 by another transaction
        sharedGoal.setCurrentAmount(BigDecimal.valueOf(1000));
        goalRepository.saveAndFlush(sharedGoal);

        // Confirm attempt must fail
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                withdrawalRequestService.confirmWithdrawal(created.getId(), memberA.getPhoneNumber())
        );
        assertTrue(ex.getMessage().contains("Insufficient goal balance"));

        // Balance remains unchanged at 1000
        Goal reloadedGoal = goalRepository.findById(sharedGoal.getId()).orElseThrow();
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(reloadedGoal.getCurrentAmount()));

        // Request was not completed
        WithdrawalRequest wr = withdrawalRequestRepository.findById(created.getId()).orElseThrow();
        assertEquals(WithdrawalStatus.APPROVED, wr.getStatus());
    }

    @Test
    @DisplayName("Cancel request: Requester can cancel pending request")
    void cancelRequest_Requester_CancelsRequest() {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());

        WithdrawalRequestResponse cancelled = withdrawalRequestService.cancelRequest(created.getId(), memberA.getPhoneNumber());
        assertEquals(WithdrawalStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("HTTP Controller & Step-Up: Confirm endpoint enforces X-Step-Up-Token")
    void controller_ConfirmEndpoint_RequiresStepUpToken() throws Exception {
        CreateWithdrawalRequest req = CreateWithdrawalRequest.builder()
                .goalId(sharedGoal.getId())
                .amount(BigDecimal.valueOf(500))
                .reason("Deposit")
                .build();
        WithdrawalRequestResponse created = withdrawalRequestService.createRequest(sharedGoal.getId(), req, memberA.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberB.getPhoneNumber());
        withdrawalRequestService.approveRequest(created.getId(), memberC.getPhoneNumber());

        String accessToken = jwtUtil.generateAccessToken(memberA.getPhoneNumber());

        // Attempt without X-Step-Up-Token -> 400 Bad Request
        mockMvc.perform(post("/api/withdrawal-requests/" + created.getId() + "/confirm")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing X-Step-Up-Token header"));

        // Generate valid step-up token
        String stepUpToken = jwtUtil.generateStepUpToken(memberA.getPhoneNumber(), JwtUtil.FINANCIAL_TRANSACTION_SCOPE);

        // Attempt with valid X-Step-Up-Token -> 200 OK
        mockMvc.perform(post("/api/withdrawal-requests/" + created.getId() + "/confirm")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Step-Up-Token", stepUpToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("HTTP Controller: Endpoints support creating and approving withdrawal requests via /api/withdrawal-requests")
    void controller_CreateAndApproveEndpointsWork() throws Exception {
        String accessTokenA = jwtUtil.generateAccessToken(memberA.getPhoneNumber());
        String accessTokenB = jwtUtil.generateAccessToken(memberB.getPhoneNumber());

        // POST /api/withdrawal-requests
        String reqJson = String.format("{\"goalId\": \"%s\", \"amount\": 300.0, \"reason\": \"Flight deposit\"}", sharedGoal.getId());
        String responseContent = mockMvc.perform(post("/api/withdrawal-requests")
                        .header("Authorization", "Bearer " + accessTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(300.0))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        WithdrawalRequestResponse created = objectMapper.readTree(responseContent).get("data").traverse(objectMapper).readValueAs(WithdrawalRequestResponse.class);

        // POST /api/withdrawal-requests/{requestId}/approve
        mockMvc.perform(post("/api/withdrawal-requests/" + created.getId() + "/approve")
                        .header("Authorization", "Bearer " + accessTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvedCount").value(1));
    }
}
