package com.example.genz.Service.GoalService;

import com.example.genz.DTO.GoalDTO.ContributionPlanStatusResponse;
import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.SavingPlanResponse;
import com.example.genz.DTO.GoalDTO.UpdateGoalRequest;
import com.example.genz.DTO.GoalDTO.UpdateSavingPlanRequest;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Entity.Goal.SavingPlan;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Entity.Transaction.GoalContribution;
import com.example.genz.Enums.CompletionStatus;
import com.example.genz.Enums.GoalStatus;
import com.example.genz.Enums.GoalType;
import com.example.genz.Enums.MemberRole;
import com.example.genz.Enums.MemberStatus;
import com.example.genz.Enums.PeriodStatus;
import com.example.genz.Enums.SavingPlanStatus;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalContributionRepository;
import com.example.genz.Repository.GoalRepository.GoalMemberRepository;
import com.example.genz.Repository.GoalRepository.GoalRepository;
import com.example.genz.Repository.GoalRepository.SavingPlanRepository;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.exception.BadRequestException;
import com.example.genz.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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

    @Override
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        GoalType goalType = GoalType.FLEXIBLE;

        validateContributionPlanFields(request);

        Goal goal = new Goal();
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setRemaining(request.getTargetAmount());
        goal.setTargetDate(request.getTargetDate());
        goal.setGoalType(goalType);
        goal.setCategory(request.getCategory());
        goal.setImageUrl(request.getImageUrl());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setOwner(owner);
        goal.setCreatedAt(LocalDateTime.now());

        Goal saved = goalRepository.save(goal);

        GrouplWallet wallet = new GrouplWallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(owner);
        wallet.setGoal(saved);
        wallet.setCurrency(request.getCurrency());
        goalWalletRepository.save(wallet);

        addOwnerAsMember(saved, owner);

        if (request.getContributionAmount() != null && request.getFrequency() != null) {
            SavingPlan plan = new SavingPlan();
            plan.setGoal(saved);
            plan.setContributionAmount(request.getContributionAmount());
            plan.setFrequency(request.getFrequency());
            LocalDate initialDueDate = request.getFirstDueDate() != null ? request.getFirstDueDate() : request.getNextContributionDate();
            if (initialDueDate == null) {
                initialDueDate = LocalDate.now();
            }
            plan.setNextContributionDate(initialDueDate);
            plan.setStatus(SavingPlanStatus.ACTIVE);
            plan.setContributedThisPeriod(BigDecimal.ZERO);
            savingPlanRepository.save(plan);
        }
        return toGoalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getMyGoals(String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        return goalRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
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

        if (request.getContributionAmount() != null) {
            if (request.getContributionAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("contributionAmount must be greater than 0");
            }
            plan.setContributionAmount(request.getContributionAmount());
        }
        if (request.getFrequency() != null) {
            plan.setFrequency(request.getFrequency());
        }
        if (request.getNextContributionDate() != null) {
            plan.setNextContributionDate(request.getNextContributionDate());
        }

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

        if (request.getGoalName() != null) {
            goal.setGoalName(request.getGoalName());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
            BigDecimal current = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
            BigDecimal rem = request.getTargetAmount().subtract(current);
            goal.setRemaining(rem.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : rem);
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getCategory() != null) {
            goal.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            goal.setImageUrl(request.getImageUrl());
        }

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

        return ContributionPlanStatusResponse.builder()
                .goalId(goal.getId())
                .targetAmount(targetAmount)
                .currentAmount(currentAmount)
                .remainingAmount(remainingAmount)
                .contributionAmount(contributionAmount)
                .frequency(plan.getFrequency())
                .nextDueDate(plan.getNextContributionDate())
                .contributedThisPeriod(contributedThisPeriod)
                .remainingThisPeriod(remainingThisPeriod)
                .periodStatus(periodStatus)
                .planStatus(plan.getStatus())
                .estimatedCompletionDate(estimatedCompletionDate)
                .build();
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
        return SavingPlanResponse.builder()
                .frequency(plan.getFrequency())
                .contributionAmount(plan.getContributionAmount())
                .estimatedCompletionDate(estimateCompletionDate(goal, plan, memberCount))
                .nextContributionDate(plan.getNextContributionDate())
                .status(plan.getStatus())
                .contributedThisPeriod(plan.getContributedThisPeriod())
                .progress(calculateProgress(goal))
                .build();
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

    private Integer calculateProgressPercentage(BigDecimal currentAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        if (currentAmount == null || currentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        int progress = currentAmount.multiply(ONE_HUNDRED)
                .divide(targetAmount, 0, RoundingMode.DOWN)
                .intValue();
        return Math.min(100, Math.max(0, progress));
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

        Integer progressPercentage = calculateProgressPercentage(current, target);
        LocalDate expectedDate = calculateExpectedCompletionDate(goal);
        LocalDate estimatedDate = calculateEstimatedCompletionDate(goal);
        CompletionStatus completionStatus = calculateCompletionStatus(expectedDate, estimatedDate);
        Long daysDifference = calculateDaysDifference(expectedDate, estimatedDate);

        GoalResponse.OwnerInfo ownerInfo = null;
        if (goal.getOwner() != null) {
            ownerInfo = GoalResponse.OwnerInfo.builder()
                    .userId(goal.getOwner().getId())
                    .name(goal.getOwner().getFullName())
                    .build();
        }

        List<GoalMember> activeMembers = null;
        if (goalMemberRepository != null) {
            activeMembers = goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE);
            if (activeMembers == null) {
                activeMembers = goalMemberRepository.findByGoalAndStatus(goal, MemberStatus.ACTIVE);
            }
        }
        List<GoalResponse.MemberInfo> memberInfos = (activeMembers != null) ? activeMembers.stream()
                .map(m -> GoalResponse.MemberInfo.builder()
                        .userId(m.getUser() != null ? m.getUser().getId() : null)
                        .name(m.getUser() != null ? m.getUser().getFullName() : null)
                        .role(m.getRole())
                        .build())
                .toList() : java.util.Collections.emptyList();

        return GoalResponse.builder()
                .goalId(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(rem)
                .progressPercentage(progressPercentage)
                .targetDate(goal.getTargetDate())
                .expectedCompletionDate(expectedDate)
                .estimatedCompletionDate(estimatedDate)
                .completionStatus(completionStatus)
                .daysDifference(daysDifference)
                .category(goal.getCategory())
                .goalType(goal.getGoalType() != null ? goal.getGoalType() : GoalType.FLEXIBLE)
                .imageUrl(goal.getImageUrl())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .completedAt(goal.getCompletedAt())
                .owner(ownerInfo)
                .members(memberInfos)
                .build();
    }
}
