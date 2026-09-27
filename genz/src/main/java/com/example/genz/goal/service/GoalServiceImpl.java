package com.example.genz.goal.service;

import com.example.genz.goal.dto.ContributionPlanStatusResponse;
import com.example.genz.goal.dto.CreateGoalRequest;
import com.example.genz.goal.dto.GoalResponse;
import com.example.genz.goal.dto.SavingPlanResponse;
import com.example.genz.goal.dto.UpdateGoalRequest;
import com.example.genz.goal.dto.UpdateSavingPlanRequest;
import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.entity.SavingPlan;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.reward.enums.BadgeRequirementType;
import com.example.genz.reward.enums.CompletionStatus;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.GoalType;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.transaction.enums.PeriodStatus;
import com.example.genz.goal.enums.SavingPlanStatus;
import com.example.genz.goal.mapper.GoalMapper;
import com.example.genz.goal.mapper.SavingPlanMapper;
import com.example.genz.wallet.mapper.WalletMapper;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.repository.SavingPlanRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.reward.service.AchievementService;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final SavingPlanRepository savingPlanRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final AchievementService achievementService;
    private final com.example.genz.challenge.service.ChallengeCompletionService challengeCompletionService;
    private final GoalMapper goalMapper;
    private final SavingPlanMapper savingPlanMapper;
    private final WalletMapper walletMapper;

    @Override
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        GoalType goalType = GoalType.FLEXIBLE;

        validateContributionPlanFields(request);

        Goal goal = goalMapper.toEntity(request, owner, goalType);
        Goal saved = goalRepository.save(goal);

        GrouplWallet wallet = walletMapper.toGoalWalletEntity(owner, saved, request.getCurrency());
        goalWalletRepository.save(wallet);

        addOwnerAsMember(saved, owner);

        if (request.getContributionAmount() != null && request.getFrequency() != null) {
            LocalDate initialDueDate = request.getFirstDueDate() != null ? request.getFirstDueDate() : request.getNextContributionDate();
            SavingPlan plan = savingPlanMapper.toEntity(request, saved, initialDueDate);
            if (plan != null) {
                savingPlanRepository.save(plan);
            }
        }

        try {
            achievementService.evaluateAndUnlockAchievements(owner, BadgeRequirementType.FIRST_GOAL_CREATED, Map.of("createdGoalCount", 1));
            achievementService.evaluateAndUnlockAchievements(owner, BadgeRequirementType.MULTI_GOAL_MASTER, Map.of());
            challengeCompletionService.evaluateGoalChallenges(owner);
        } catch (Exception ignored) {}

        return toGoalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getMyGoals(String phoneNumber, GoalStatus status) {
        User user = getUserByPhone(phoneNumber);
        List<Goal> goals = (status != null)
                ? goalRepository.findAccessibleGoalsByStatus(user, status)
                : goalRepository.findAccessibleGoals(user);
        return goals.stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoal(UUID goalId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserIdAndStatus(goal.getId(), user.getId(), MemberStatus.ACTIVE);

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("You do not have access to this goal");
        }

        return toGoalResponse(goal);
    }

    @Override
    @Transactional(readOnly = true)
    public SavingPlanResponse getSavingPlan(UUID goalId, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findByIdAndOwner(goalId, owner)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        SavingPlan plan = savingPlanRepository.findByGoalId(goal.getId())
                .orElseThrow(() -> new ResourceNoteFoundException("Saving plan not found for this goal"));

        return toSavingPlanResponse(goal, plan);
    }

    @Override
    @Transactional
    public SavingPlanResponse updateSavingPlan(UUID goalId, UpdateSavingPlanRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findByIdAndOwner(goalId, owner)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        SavingPlan plan = savingPlanRepository.findByGoalId(goal.getId())
                .orElseThrow(() -> new ResourceNoteFoundException("Saving plan not found for this goal"));

        if (request.getContributionAmount() != null && request.getContributionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("contributionAmount must be greater than 0");
        }

        savingPlanMapper.updateEntity(plan, request);

        SavingPlan updated = savingPlanRepository.save(plan);
        return toSavingPlanResponse(goal, updated);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        if (!goal.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have permission to update this goal");
        }

        goalMapper.updateEntity(goal, request);

        Goal saved = goalRepository.save(goal);
        return toGoalResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGoal(UUID goalId, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        if (!goal.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this goal");
        }

        goal.setStatus(GoalStatus.CANCELLED);
        goalRepository.save(goal);
    }

    private void validateContributionPlanFields(CreateGoalRequest request) {
        if (request.getContributionAmount() != null) {
            if (request.getContributionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("contributionAmount must be greater than 0");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ContributionPlanStatusResponse getContributionPlanStatus(UUID goalId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserId(goal.getId(), user.getId());

        if (!isOwner && !isMember) {
            throw new AccessDeniedException("Access denied: user is not a member or owner of this goal");
        }

        SavingPlan plan = savingPlanRepository.findByGoalId(goal.getId())
                .orElseThrow(() -> new ResourceNoteFoundException("No contribution plan found for goal: " + goal.getGoalName()));

        BigDecimal targetAmount = goal.getTargetAmount();
        BigDecimal currentAmount = goal.getCurrentAmount();
        BigDecimal remainingAmount = targetAmount.subtract(currentAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        BigDecimal contributionAmount = plan.getContributionAmount();
        BigDecimal contributedThisPeriod = plan.getContributedThisPeriod() != null ? plan.getContributedThisPeriod() : BigDecimal.ZERO;
        BigDecimal remainingThisPeriod = contributionAmount.subtract(contributedThisPeriod);
        if (remainingThisPeriod.compareTo(BigDecimal.ZERO) < 0) {
            remainingThisPeriod = BigDecimal.ZERO;
        }

        LocalDate today = LocalDate.now();
        PeriodStatus periodStatus;
        if (goal.getStatus() == GoalStatus.COMPLETED || plan.getStatus() == SavingPlanStatus.COMPLETED) {
            periodStatus = PeriodStatus.COMPLETED;
        } else if (contributedThisPeriod.compareTo(contributionAmount) >= 0) {
            periodStatus = PeriodStatus.COMPLETED;
        } else if (today.isAfter(plan.getNextContributionDate())) {
            periodStatus = PeriodStatus.OVERDUE;
        } else if (contributedThisPeriod.compareTo(BigDecimal.ZERO) > 0) {
            periodStatus = PeriodStatus.PARTIALLY_COMPLETED;
        } else {
            periodStatus = PeriodStatus.PENDING;
        }

        long memberCount = goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE);
        LocalDate estimatedCompletionDate = estimateCompletionDate(goal, plan, memberCount > 0 ? memberCount : 1);

        return savingPlanMapper.toContributionPlanStatusResponse(
                goal, plan, remainingAmount, remainingThisPeriod, periodStatus, estimatedCompletionDate
        );
    }

    private void addOwnerAsMember(Goal goal, User owner) {
        GoalMember member = new GoalMember();
        member.setGoal(goal);
        member.setUser(owner);
        member.setRole(MemberRole.OWNER);
        member.setStatus(MemberStatus.ACTIVE);
        goalMemberRepository.save(member);
    }

    private SavingPlanResponse toSavingPlanResponse(Goal goal, SavingPlan plan) {
        long memberCount = goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE);
        LocalDate estimatedDate = estimateCompletionDate(goal, plan, memberCount);
        int progress = calculateProgress(goal);
        return savingPlanMapper.toSavingPlanResponse(plan, progress, estimatedDate);
    }

    private LocalDate estimateCompletionDate(Goal goal, SavingPlan plan, long memberCount) {
        BigDecimal remainingAmount = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return LocalDate.now();
        }
        if (memberCount == 0) {
            return null;
        }

        BigDecimal perPeriodContribution = plan.getContributionAmount().multiply(BigDecimal.valueOf(memberCount));
        BigDecimal periods = remainingAmount.divide(perPeriodContribution, 0, RoundingMode.CEILING);
        long periodCount = periods.longValue();

        return switch (plan.getFrequency()) {
            case WEEKLY -> LocalDate.now().plusWeeks(periodCount);
            case MONTHLY -> LocalDate.now().plusMonths(periodCount);
        };
    }

    private int calculateProgress(Goal goal) {
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return goal.getCurrentAmount()
                .multiply(ONE_HUNDRED)
                .divide(goal.getTargetAmount(), 0, RoundingMode.DOWN)
                .intValue();
    }

    private User getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
    }

    private Double calculateProgressPercentage(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        BigDecimal progress = currentAmount.multiply(ONE_HUNDRED)
                .divide(targetAmount, 4, RoundingMode.HALF_UP);
        double val = progress.doubleValue();
        return Math.min(100.0, Math.max(0.0, val));
    }

    private LocalDate calculateExpectedCompletionDate(Goal goal) {
        if (goal.getTargetDate() != null) {
            return goal.getTargetDate();
        }
        SavingPlan plan = savingPlanRepository != null ? savingPlanRepository.findByGoalId(goal.getId()).orElse(null) : null;
        if (plan != null && plan.getContributionAmount() != null && plan.getContributionAmount().compareTo(BigDecimal.ZERO) > 0 && plan.getFrequency() != null) {
            long memberCount = goalMemberRepository != null ? goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE) : 1;
            return estimateCompletionDate(goal, plan, memberCount > 0 ? memberCount : 1);
        }
        return null;
    }

    private LocalDate calculateEstimatedCompletionDate(Goal goal) {
        BigDecimal target = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;

        if (goal.getStatus() == GoalStatus.COMPLETED || current.compareTo(target) >= 0) {
            return goal.getCompletedAt() != null ? goal.getCompletedAt().toLocalDate() : LocalDate.now();
        }

        List<GoalContribution> contributions = goalContributionRepository != null ? goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId()) : null;
        if (contributions == null || contributions.isEmpty()) {
            return null;
        }

        BigDecimal totalContributed = contributions.stream()
                .map(GoalContribution::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalContributed.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        LocalDate startDate = contributions.get(0).getContributedAt() != null
                ? contributions.get(0).getContributedAt().toLocalDate()
                : (goal.getCreatedAt() != null ? goal.getCreatedAt().toLocalDate() : LocalDate.now());

        long daysElapsed = java.time.temporal.ChronoUnit.DAYS.between(startDate, LocalDate.now());
        if (daysElapsed < 1) {
            return null;
        }

        BigDecimal dailyRate = totalContributed.divide(BigDecimal.valueOf(daysElapsed), 6, RoundingMode.HALF_UP);
        if (dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal remainingAmount = target.subtract(current);
        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return LocalDate.now();
        }

        long daysToComplete = remainingAmount.divide(dailyRate, 0, RoundingMode.CEILING).longValue();
        return LocalDate.now().plusDays(daysToComplete);
    }

    private CompletionStatus calculateCompletionStatus(LocalDate expected, LocalDate estimated) {
        if (expected == null || estimated == null) {
            return null;
        }
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(estimated, expected);
        long absDiff = Math.abs(daysDiff);
        if (absDiff <= 7) {
            return CompletionStatus.ON_TRACK;
        } else if (daysDiff > 7) {
            return CompletionStatus.AHEAD;
        } else {
            return CompletionStatus.BEHIND;
        }
    }

    private Long calculateDaysDifference(LocalDate expected, LocalDate estimated) {
        if (expected == null || estimated == null) {
            return null;
        }
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(estimated, expected);
        return Math.abs(daysDiff);
    }

    private GoalResponse toGoalResponse(Goal goal) {
        GrouplWallet wallet = goalWalletRepository != null ? goalWalletRepository.findByGoal(goal).orElse(null) : null;
        BigDecimal target = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal current = (wallet != null && wallet.getBalance() != null)
                ? wallet.getBalance()
                : (goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);
        BigDecimal rem = target.subtract(current);
        if (rem.compareTo(BigDecimal.ZERO) < 0) {
            rem = BigDecimal.ZERO;
        }

        Double progressPercentage = calculateProgressPercentage(current, target);
        LocalDate expectedDate = calculateExpectedCompletionDate(goal);
        LocalDate estimatedDate = calculateEstimatedCompletionDate(goal);
        CompletionStatus completionStatus = calculateCompletionStatus(expectedDate, estimatedDate);
        Long daysDifference = calculateDaysDifference(expectedDate, estimatedDate);

        List<GoalMember> activeMembers = null;
        if (goalMemberRepository != null) {
            activeMembers = goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE);
            if (activeMembers == null) {
                activeMembers = goalMemberRepository.findByGoalAndStatus(goal, MemberStatus.ACTIVE);
            }
        }

        return goalMapper.toGoalResponse(
                goal,
                current,
                rem,
                progressPercentage,
                expectedDate,
                estimatedDate,
                completionStatus,
                daysDifference,
                activeMembers
        );
    }
}
