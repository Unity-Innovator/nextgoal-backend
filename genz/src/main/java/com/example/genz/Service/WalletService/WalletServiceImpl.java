package com.example.genz.service.WalletService;

import com.example.genz.dto.WalletDTO.DepositRequest;
import com.example.genz.dto.WalletDTO.DepositResponse;
import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Goal.Goal;
import com.example.genz.entity.Goal.GoalMember;
import com.example.genz.entity.Goal.SavingPlan;
import com.example.genz.entity.Transaction.GoalContribution;
import com.example.genz.entity.Transaction.WalletTransaction;
import com.example.genz.entity.Wallet.GrouplWallet;
import com.example.genz.enums.ContributionFrequency;
import com.example.genz.enums.GoalStatus;
import com.example.genz.enums.MemberRole;
import com.example.genz.enums.MemberStatus;
import com.example.genz.enums.SavingPlanStatus;
import com.example.genz.enums.TransactionStatus;
import com.example.genz.enums.TransactionType;
import com.example.genz.mapper.WalletMapper;
import com.example.genz.repository.AuthRepository.UserRepository;
import com.example.genz.repository.GoalRepository.GoalContributionRepository;
import com.example.genz.repository.GoalRepository.GoalMemberRepository;
import com.example.genz.repository.GoalRepository.GoalRepository;
import com.example.genz.repository.GoalRepository.SavingPlanRepository;
import com.example.genz.repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.repository.TransactionRepository.WalletTransactionRepository;
import com.example.genz.service.NotificationService.NotificationService;
import com.example.genz.exception.BadRequestException;
import com.example.genz.exception.ResourceNoteFoundException;

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
