package com.example.genz.wallet.service;

import com.example.genz.transaction.dto.TransactionPageResponse;
import com.example.genz.transaction.dto.TransactionResponse;
import com.example.genz.transaction.dto.TransactionUserResponse;
import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.wallet.dto.TopUpOptionsResponse;
import com.example.genz.wallet.dto.TopUpRequest;
import com.example.genz.wallet.dto.TopUpResponse;
import com.example.genz.wallet.dto.WalletResponse;
import com.example.genz.wallet.dto.WalletSummaryResponse;
import com.example.genz.wallet.entity.Wallet;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.entity.SavingPlan;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.goal.enums.ContributionFrequency;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.enums.SavingPlanStatus;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.TransactionDirection;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.wallet.mapper.WalletMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.repository.SavingPlanRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.wallet.repository.WalletRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.notification.service.NotificationService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;

import com.example.genz.reward.service.GoalMilestoneRewardService;
import com.example.genz.challenge.service.ChallengeCompletionService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SavingPlanRepository savingPlanRepository;
    private final NotificationService notificationService;
    private final WalletMapper walletMapper;
    private final GoalMilestoneRewardService goalMilestoneRewardService;
    private final ChallengeCompletionService challengeCompletionService;

    @Override
    @Transactional
    public WalletResponse getWalletByPhoneNumber(String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> getOrCreateWallet(user));
        WalletResponse response = walletMapper.toWalletResponse(wallet);
        calculateMonthChange(user, wallet, response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryResponse getWalletSummary(String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> getOrCreateWallet(user));
        WalletResponse walletResponse = walletMapper.toWalletResponse(wallet);
        calculateMonthChange(user, wallet, walletResponse);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<TransactionResponse> recent = walletTransactionRepository.findByUser_Id(user.getId(), pageRequest)
                .map(this::toTransactionResponse)
                .getContent();

        return WalletSummaryResponse.builder()
                .walletId(walletResponse.getWalletId())
                .balance(walletResponse.getBalance())
                .currency(walletResponse.getCurrency())
                .monthChangePercent(walletResponse.getMonthChangePercent())
                .monthChangeAmount(walletResponse.getMonthChangeAmount())
                .recentTransactions(recent)
                .build();
    }

    @Override
    @Transactional
    public Wallet getOrCreateWallet(User user) {
        return walletRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Wallet newWallet = Wallet.builder()
                            .user(user)
                            .balance(BigDecimal.ZERO)
                            .currency(Currency.USD)
                            .build();
                    return walletRepository.save(newWallet);
                });
    }

    @Override
    @Transactional
    public TopUpResponse topUp(TopUpRequest request, String phoneNumber) {
        User user = userRepository.findByPhoneNumberForUpdate(phoneNumber)
                .or(() -> userRepository.findByPhoneNumber(phoneNumber))
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone: " + phoneNumber));
        Wallet existingWallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> getOrCreateWallet(user));
        Wallet wallet = walletRepository.findByIdForUpdate(existingWallet.getId())
                .orElse(existingWallet);

        String currencyStr = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().trim().toUpperCase()
                : "USD";

        // Convert KHR to USD if currency is KHR (approx 4100 KHR = 1 USD)
        BigDecimal creditAmountInUsd = request.getAmount();
        if ("KHR".equals(currencyStr)) {
            creditAmountInUsd = request.getAmount().divide(BigDecimal.valueOf(4100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal balanceBefore = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal balanceAfter = balanceBefore.add(creditAmountInUsd);
        wallet.setBalance(balanceAfter);
        Wallet savedWallet = walletRepository.save(wallet);

        String bankName = (request.getBank() != null && !request.getBank().isBlank()) ? request.getBank().trim() : null;
        String description;
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            description = request.getDescription();
        } else if (bankName != null) {
            description = "Top Up — " + bankName;
        } else {
            description = "Wallet top up";
        }

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(savedWallet)
                .user(user)
                .goal(null)
                .amount(creditAmountInUsd)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .direction(TransactionDirection.IN)
                .type(TransactionType.TOP_UP)
                .status(TransactionStatus.SUCCESS)
                .description(description)
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);

        String referenceId = generateTopUpReference();

        return TopUpResponse.builder()
                .walletId(savedWallet.getId())
                .referenceId(referenceId)
                .amount(request.getAmount())
                .currency(currencyStr)
                .bank(bankName)
                .paymentMethod(request.getPaymentMethod())
                .balance(balanceAfter)
                .newBalance(balanceAfter)
                .completedAt(transaction.getCreatedAt())
                .build();
    }

    @Override
    public TopUpOptionsResponse getTopUpOptions() {
        return TopUpOptionsResponse.builder()
                .banks(List.of(
                        TopUpOptionsResponse.BankOption.builder().code("ABA").name("ABA Bank").logoUrl("assets/banks/aba.png").build(),
                        TopUpOptionsResponse.BankOption.builder().code("ACLEDA").name("ACLEDA Bank").logoUrl("assets/banks/acleda.png").build(),
                        TopUpOptionsResponse.BankOption.builder().code("WING").name("Wing Bank").logoUrl("assets/banks/wing.png").build()
                ))
                .currencies(List.of("USD", "KHR"))
                .quickAmounts(List.of(
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(250),
                        BigDecimal.valueOf(500),
                        BigDecimal.valueOf(1000),
                        BigDecimal.valueOf(2000)
                ))
                .paymentMethods(List.of(
                        TopUpOptionsResponse.PaymentMethodOption.builder().code("BANK_TRANSFER").name("Bank Transfer").icon("account_balance").build(),
                        TopUpOptionsResponse.PaymentMethodOption.builder().code("QR_CODE").name("QR Code").icon("qr_code_scanner").build(),
                        TopUpOptionsResponse.PaymentMethodOption.builder().code("DEBIT_CARD").name("Debit Card").icon("credit_card").build()
                ))
                .exchangeRateKhrToUsd(4100.0)
                .build();
    }

    private void calculateMonthChange(User user, Wallet wallet, WalletResponse response) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<WalletTransaction> recentTxs = walletTransactionRepository.findByUser_Id(user.getId(), null).getContent();

        BigDecimal netInflow = BigDecimal.ZERO;
        for (WalletTransaction tx : recentTxs) {
            if (tx.getCreatedAt() != null && tx.getCreatedAt().isAfter(thirtyDaysAgo) && tx.getStatus() == TransactionStatus.SUCCESS) {
                if (tx.getDirection() == TransactionDirection.IN) {
                    netInflow = netInflow.add(tx.getAmount());
                } else if (tx.getDirection() == TransactionDirection.OUT) {
                    netInflow = netInflow.subtract(tx.getAmount());
                }
            }
        }

        BigDecimal currentBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal startBalance = currentBalance.subtract(netInflow);

        double percent;
        if (startBalance.compareTo(BigDecimal.ZERO) > 0) {
            percent = netInflow.divide(startBalance, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        } else if (netInflow.compareTo(BigDecimal.ZERO) > 0) {
            percent = 100.0;
        } else {
            percent = 0.0;
        }

        response.setMonthChangeAmount(netInflow);
        response.setMonthChangePercent(percent);
    }

    private String generateTopUpReference() {
        int year = LocalDateTime.now().getYear();
        int randomSuffix = java.util.concurrent.ThreadLocalRandom.current().nextInt(10000, 100000);
        return String.format("REF-%d-TOP-%05d", year, randomSuffix);
    }

    @Override
    @Transactional
    public DepositResponse deposit(DepositRequest request, String phoneNumber) {
        // Lock user row and wallet row to prevent concurrent double-spending
        User user = userRepository.findByPhoneNumberForUpdate(phoneNumber)
                .or(() -> userRepository.findByPhoneNumber(phoneNumber))
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone: " + phoneNumber));

        Wallet userWallet = walletRepository.findByUserId(user.getId())
                .orElseGet(() -> getOrCreateWallet(user));
        userWallet = walletRepository.findByIdForUpdate(userWallet.getId())
                .orElse(userWallet);

        BigDecimal availableBalance = userWallet.getBalance() != null ? userWallet.getBalance() : BigDecimal.ZERO;
        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new BadRequestException("Insufficient wallet balance for deposit. Available: $" + availableBalance + ", requested: $" + request.getAmount());
        }

        // Serialize balance-changing operations for the same goal. Without the
        // row lock, concurrent deposits can overwrite each other's totals.
        Goal goal = goalRepository.findByIdForUpdate(request.getGoalId())
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found with ID: " + request.getGoalId()));

        boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), user.getId());

        if (!isOwner && !isMember) {
            throw new BadRequestException("User is not a member or owner of this goal");
        }

        // Deduct money from authenticated user's personal wallet
        BigDecimal walletBalanceBefore = availableBalance;
        BigDecimal walletBalanceAfter = walletBalanceBefore.subtract(request.getAmount());
        userWallet.setBalance(walletBalanceAfter);
        walletRepository.save(userWallet);

        // Increase goal wallet balance
        GrouplWallet goalWallet = goalWalletRepository.findByGoal(goal)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal wallet not found for goal: " + goal.getGoalName()));

        BigDecimal goalWalletBalance = goalWallet.getBalance() != null ? goalWallet.getBalance() : BigDecimal.ZERO;
        BigDecimal newGoalWalletBalance = goalWalletBalance.add(request.getAmount());
        goalWallet.setBalance(newGoalWalletBalance);
        goalWalletRepository.save(goalWallet);

        // Increase goal current amount
        BigDecimal currentAmount = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal newCurrentAmount = currentAmount.add(request.getAmount());
        goal.setCurrentAmount(newCurrentAmount);

        BigDecimal targetAmount = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = targetAmount.subtract(newCurrentAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }
        goal.setRemaining(remainingAmount);

        boolean isGoalCompleted = newCurrentAmount.compareTo(targetAmount) >= 0;
        if (isGoalCompleted && goal.getStatus() != GoalStatus.COMPLETED) {
            goal.setStatus(GoalStatus.COMPLETED);
            goal.setCompletedAt(LocalDateTime.now());
        }
        goalRepository.save(goal);

        // Process Saving Contribution Plan if exists
        SavingPlan plan = savingPlanRepository.findByGoalId(goal.getId()).orElse(null);
        if (plan != null && plan.getStatus() == SavingPlanStatus.ACTIVE) {
            if (isGoalCompleted) {
                plan.setStatus(SavingPlanStatus.COMPLETED);
            } else {
                BigDecimal currentContributed = plan.getContributedThisPeriod() != null ? plan.getContributedThisPeriod() : BigDecimal.ZERO;
                BigDecimal updatedContributed = currentContributed.add(request.getAmount());
                BigDecimal expected = plan.getContributionAmount() != null ? plan.getContributionAmount() : BigDecimal.ZERO;

                if (updatedContributed.compareTo(expected) >= 0) {
                    plan.setContributedThisPeriod(BigDecimal.ZERO);
                    if (plan.getFrequency() == ContributionFrequency.WEEKLY) {
                        plan.setNextContributionDate(plan.getNextContributionDate().plusWeeks(1));
                    } else {
                        plan.setNextContributionDate(plan.getNextContributionDate().plusMonths(1));
                    }
                } else {
                    plan.setContributedThisPeriod(updatedContributed);
                }
            }
            savingPlanRepository.save(plan);
        }

        GoalMember member = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), user.getId())
                .orElseGet(() -> {
                    GoalMember newMember = new GoalMember();
                    newMember.setGoal(goal);
                    newMember.setUser(user);
                    newMember.setRole(isOwner ? MemberRole.OWNER : MemberRole.MEMBER);
                    newMember.setStatus(MemberStatus.ACTIVE);
                    return goalMemberRepository.save(newMember);
                });

        GoalContribution contribution = walletMapper.toGoalContributionEntity(goal, member, request.getAmount(), "Manual deposit");
        GoalContribution savedContribution = goalContributionRepository.save(contribution);

        // Record WalletTransaction on User's Personal Wallet
        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(userWallet)
                .user(user)
                .goal(goal)
                .amount(request.getAmount())
                .balanceBefore(walletBalanceBefore)
                .balanceAfter(walletBalanceAfter)
                .direction(TransactionDirection.OUT)
                .type(TransactionType.GOAL_DEPOSIT)
                .status(TransactionStatus.SUCCESS)
                .description("Deposit to goal: " + goal.getGoalName())
                .createdAt(LocalDateTime.now())
                .build();
        walletTransactionRepository.save(transaction);

        challengeCompletionService.updateStreak(user);

        int coinsEarned = goalMilestoneRewardService.processMilestoneRewards(goal, user, currentAmount, newCurrentAmount);

        try {
            challengeCompletionService.evaluateDepositChallenges(user, request.getAmount());
            challengeCompletionService.evaluateGoalMilestoneChallenges(user, goal, currentAmount, newCurrentAmount);
        } catch (Exception ignored) {}

        notificationService.createGoalContributionNotifications(goal, savedContribution, user);

        Double progressPercentage = 0.0;
        if (targetAmount.compareTo(BigDecimal.ZERO) > 0) {
            double rawProgress = newCurrentAmount.divide(targetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
            progressPercentage = Math.min(100.0, rawProgress);
        }

        return walletMapper.toDepositResponse(
                true,
                "Deposit successful.",
                walletBalanceAfter,
                coinsEarned,
                newCurrentAmount,
                targetAmount,
                progressPercentage,
                goal.getStatus()
        );
    }

    private User getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
    }

    private TransactionResponse toTransactionResponse(WalletTransaction t) {
        String typeStr = (t.getType() != null) ? t.getType().name() : "TOP_UP";

        TransactionUserResponse userResp = null;
        if (t.getUser() != null) {
            userResp = TransactionUserResponse.builder()
                    .id(t.getUser().getId())
                    .fullName(t.getUser().getFullName())
                    .build();
        }

        String currencyStr = (t.getWallet() != null && t.getWallet().getCurrency() != null)
                ? t.getWallet().getCurrency().name()
                : "USD";

        String statusStr = (t.getStatus() != null) ? t.getStatus().name() : "SUCCESS";
        String directionStr = (t.getDirection() != null)
                ? t.getDirection().name()
                : (t.getType() != null ? t.getType().getDefaultDirection().name() : "OUT");

        UUID goalId = t.getGoal() != null ? t.getGoal().getId() : null;
        String goalName = t.getGoal() != null ? t.getGoal().getGoalName() : null;

        return TransactionResponse.builder()
                .id(t.getId())
                .user(userResp)
                .type(typeStr)
                .amount(t.getAmount())
                .direction(directionStr)
                .goalId(goalId)
                .goalName(goalName)
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .currency(currencyStr)
                .status(statusStr)
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
