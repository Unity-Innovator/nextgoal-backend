package com.example.genz.goal;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.enums.Gender;
import com.example.genz.auth.enums.UserRole;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalCategory;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.GoalType;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.transaction.service.TransactionService;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:genz_tx_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=test-only-jwt-secret-must-be-at-least-32-characters-long",
        "jwt.access-expiration=900000",
        "jwt.refresh-expiration=604800000",
        "jwt.step-up-expiration=300000"
})
@Transactional
class GoalTransactionIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private GoalMemberRepository goalMemberRepository;

    @Autowired
    private GoalWalletRepository goalWalletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private com.example.genz.wallet.repository.WalletRepository walletRepository;

    private User owner;
    private User memberDara;
    private User stranger;
    private Goal sharedGoal;
    private GrouplWallet wallet;
    private com.example.genz.wallet.entity.Wallet ownerWallet;
    private com.example.genz.wallet.entity.Wallet memberDaraWallet;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(User.builder()
                .fullName("Chea Teng")
                .phoneNumber("+85512000001")
                .dateOfBirth(LocalDate.of(2000, 1, 1))
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        memberDara = userRepository.save(User.builder()
                .fullName("Dara")
                .phoneNumber("+85512000002")
                .dateOfBirth(LocalDate.of(2001, 2, 2))
                .gender(Gender.MALE)
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        ownerWallet = walletRepository.save(com.example.genz.wallet.entity.Wallet.builder()
                .user(owner)
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.USD)
                .build());

        memberDaraWallet = walletRepository.save(com.example.genz.wallet.entity.Wallet.builder()
                .user(memberDara)
                .balance(new BigDecimal("1000.00"))
                .currency(Currency.USD)
                .build());

        stranger = userRepository.save(User.builder()
                .fullName("Stranger")
                .phoneNumber("+85512000003")
                .dateOfBirth(LocalDate.of(1999, 3, 3))
                .gender(Gender.FEMALE)
                .role(UserRole.USER)
                .status("ACTIVE")
                .build());

        sharedGoal = new Goal();
        sharedGoal.setGoalName("Trip to Siem Reap");
        sharedGoal.setTargetAmount(BigDecimal.valueOf(1000));
        sharedGoal.setCurrentAmount(BigDecimal.ZERO);
        sharedGoal.setRemaining(BigDecimal.valueOf(1000));
        sharedGoal.setTargetDate(LocalDate.now().plusMonths(6));
        sharedGoal.setCategory(GoalCategory.TRAVEL);
        sharedGoal.setGoalType(GoalType.FLEXIBLE);
        sharedGoal.setStatus(GoalStatus.ACTIVE);
        sharedGoal.setOwner(owner);
        sharedGoal.setCreatedAt(LocalDateTime.now());
        sharedGoal = goalRepository.save(sharedGoal);

        wallet = new GrouplWallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(owner);
        wallet.setGoal(sharedGoal);
        wallet.setCurrency(Currency.USD);
        wallet = goalWalletRepository.save(wallet);

        GoalMember ownerMember = new GoalMember();
        ownerMember.setGoal(sharedGoal);
        ownerMember.setUser(owner);
        ownerMember.setRole(MemberRole.OWNER);
        ownerMember.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(ownerMember);

        GoalMember secondMember = new GoalMember();
        secondMember.setGoal(sharedGoal);
        secondMember.setUser(memberDara);
        secondMember.setRole(MemberRole.MEMBER);
        secondMember.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(secondMember);
    }

    @Test
    @DisplayName("Deposit creates authoritative transaction with explicit DEPOSIT type and user details")
    void testDepositCreatesTransactionHistory() {
        // Chea Teng deposits 168.98
        DepositRequest req = new DepositRequest(sharedGoal.getId(), new BigDecimal("168.98"));
        walletService.deposit(req, owner.getPhoneNumber());

        TransactionPageResponse<TransactionResponse> page = transactionService.getGoalTransactions(
                sharedGoal.getId(), null, 0, 10, owner.getPhoneNumber()
        );

        assertNotNull(page);
        assertEquals(1, page.items().size());
        assertEquals(1, page.totalElements());
        assertEquals(0, page.page());

        TransactionResponse tx = page.items().get(0);
        assertNotNull(tx.getId());
        assertEquals("GOAL_DEPOSIT", tx.getType());
        assertEquals(new BigDecimal("168.98"), tx.getAmount());
        assertEquals("USD", tx.getCurrency());
        assertNotNull(tx.getUser());
        assertEquals(owner.getId(), tx.getUser().getId());
        assertEquals("Chea Teng", tx.getUser().getFullName());
        assertNotNull(tx.getCreatedAt());
    }

    @Test
    @DisplayName("Shared goal shows transactions from different members with their respective names")
    void testSharedGoalTransactionsFromDifferentMembers() {
        // Chea Teng deposits 168.98
        DepositRequest req1 = new DepositRequest(sharedGoal.getId(), new BigDecimal("168.98"));
        walletService.deposit(req1, owner.getPhoneNumber());

        // Dara deposits 50.00
        DepositRequest req2 = new DepositRequest(sharedGoal.getId(), new BigDecimal("50.00"));
        walletService.deposit(req2, memberDara.getPhoneNumber());

        // Also add a withdrawal transaction
        WalletTransaction withdrawTx = new WalletTransaction();
        withdrawTx.setWallet(ownerWallet);
        withdrawTx.setGoal(sharedGoal);
        withdrawTx.setUser(owner);
        withdrawTx.setAmount(new BigDecimal("20.00"));
        withdrawTx.setType(TransactionType.WITHDRAWAL);
        withdrawTx.setStatus(TransactionStatus.SUCCESS);
        withdrawTx.setDescription("Withdrawal from goal");
        withdrawTx.setCreatedAt(LocalDateTime.now().plusSeconds(2));
        walletTransactionRepository.save(withdrawTx);

        TransactionPageResponse<TransactionResponse> allTx = transactionService.getGoalTransactions(
                sharedGoal.getId(), "ALL", 0, 10, memberDara.getPhoneNumber()
        );

        assertEquals(3, allTx.items().size());

        // Filter DEPOSIT only
        TransactionPageResponse<TransactionResponse> depositOnly = transactionService.getGoalTransactions(
                sharedGoal.getId(), "DEPOSIT", 0, 10, memberDara.getPhoneNumber()
        );
        assertEquals(2, depositOnly.items().size());
        assertTrue(depositOnly.items().stream().allMatch(t -> "GOAL_DEPOSIT".equals(t.getType())));

        // Filter WITHDRAWAL only
        TransactionPageResponse<TransactionResponse> withdrawOnly = transactionService.getGoalTransactions(
                sharedGoal.getId(), "WITHDRAWAL", 0, 10, memberDara.getPhoneNumber()
        );
        assertEquals(1, withdrawOnly.items().size());
        assertEquals("WITHDRAWAL", withdrawOnly.items().get(0).getType());
        assertEquals(new BigDecimal("20.00"), withdrawOnly.items().get(0).getAmount());
        assertEquals("Chea Teng", withdrawOnly.items().get(0).getUser().getFullName());
    }

    @Test
    @DisplayName("Non-member cannot access goal transaction history")
    void testNonMemberAccessDenied() {
        assertThrows(AccessDeniedException.class, () ->
                transactionService.getGoalTransactions(sharedGoal.getId(), null, 0, 10, stranger.getPhoneNumber())
        );
    }
}
