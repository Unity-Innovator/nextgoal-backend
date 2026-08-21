package com.example.genz.wallet.service;

import com.example.genz.wallet.dto.DepositRequest;
import com.example.genz.wallet.dto.DepositResponse;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.entity.SavingPlan;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.goal.enums.ContributionFrequency;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.enums.SavingPlanStatus;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.wallet.mapper.WalletMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.repository.SavingPlanRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.notification.service.NotificationService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final SavingPlanRepository savingPlanRepository;
    private final NotificationService notificationService;
    private final WalletMapper walletMapper;

    @Override
    @Transactional
    public DepositResponse deposit(DepositRequest request, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        Goal goal = goalRepository.findById(request.getGoalId())
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found with ID: " + request.getGoalId()));

        boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), user.getId());

        if (!isOwner && !isMember) {
            throw new BadRequestException("User is not a member or owner of this goal");
        }

        GrouplWallet wallet = goalWalletRepository.findByGoal(goal)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal wallet not found for goal: " + goal.getGoalName()));

        BigDecimal walletBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal newBalance = walletBalance.add(request.getAmount());
        wallet.setBalance(newBalance);
        goalWalletRepository.save(wallet);

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

        WalletTransaction transaction = walletMapper.toWalletTransactionEntity(
                wallet,
                user,
                request.getAmount(),
                TransactionType.DEPOSIT,
                TransactionStatus.SUCCESS,
                "Deposit to goal: " + goal.getGoalName()
        );
        walletTransactionRepository.save(transaction);

        notificationService.createGoalContributionNotifications(goal, savedContribution, user);

        return walletMapper.toDepositResponse(true, "Deposit successful.", newBalance);
    }
}
