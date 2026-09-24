package com.example.genz.reward.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.reward.entity.GoalMilestoneReward;
import com.example.genz.reward.enums.GoalMilestone;
import com.example.genz.reward.repository.GoalMilestoneRewardRepository;
import com.example.genz.transaction.entity.GoalContribution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoalMilestoneRewardServiceImpl implements GoalMilestoneRewardService {

    private final GoalMilestoneRewardRepository goalMilestoneRewardRepository;
    private final GoalMilestoneRewardTierService tierService;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public int processMilestoneRewards(Goal goal, User depositingUser, BigDecimal previousAmount, BigDecimal newAmount) {
        if (goal == null || goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0 || depositingUser == null) {
            return 0;
        }

        BigDecimal prev = previousAmount != null ? previousAmount : BigDecimal.ZERO;
        BigDecimal next = newAmount != null ? newAmount : BigDecimal.ZERO;
        BigDecimal target = goal.getTargetAmount();

        int depositingUserCoinsEarned = 0;

        // Process milestones in ascending order
        for (GoalMilestone milestone : GoalMilestone.values()) {
            BigDecimal threshold = target.multiply(milestone.getFraction());

            // A milestone is crossed if previous amount was strictly below threshold and new amount is at or above threshold
            boolean isCrossed = prev.compareTo(threshold) < 0 && next.compareTo(threshold) >= 0;

            if (!isCrossed) {
                continue;
            }

            // Check if already awarded for this goal
            if (goalMilestoneRewardRepository.existsByGoalIdAndMilestone(goal.getId(), milestone)) {
                log.info("Milestone {} already awarded for goal {}. Skipping.", milestone, goal.getId());
                continue;
            }

            if (milestone == GoalMilestone.MILESTONE_100) {
                // 100% completion reward
                depositingUserCoinsEarned += processCompletionReward(goal, depositingUser);
            } else {
                // Intermediate milestone (25%, 50%, 75%) awarded to the member whose deposit crossed it
                int coins = tierService.getRewardCoins(target, milestone);
                if (coins > 0) {
                    GoalMilestoneReward reward = GoalMilestoneReward.builder()
                            .goal(goal)
                            .user(depositingUser)
                            .milestone(milestone)
                            .coinsEarned(coins)
                            .build();
                    goalMilestoneRewardRepository.save(reward);

                    updateUserCoinBalance(depositingUser, coins);
                    depositingUserCoinsEarned += coins;

                    log.info("Awarded {} coins to user {} for crossing milestone {} on goal {}",
                            coins, depositingUser.getId(), milestone, goal.getId());
                }
            }
        }

        return depositingUserCoinsEarned;
    }

    private int processCompletionReward(Goal goal, User depositingUser) {
        int totalCompletionCoins = tierService.getRewardCoins(goal.getTargetAmount(), GoalMilestone.MILESTONE_100);
        if (totalCompletionCoins <= 0) {
            return 0;
        }

        boolean shared = isSharedGoal(goal);
        if (!shared) {
            // Individual goal: owner gets 100% of completion reward
            User recipient = goal.getOwner() != null ? goal.getOwner() : depositingUser;
            GoalMilestoneReward reward = GoalMilestoneReward.builder()
                    .goal(goal)
                    .user(recipient)
                    .milestone(GoalMilestone.MILESTONE_100)
                    .coinsEarned(totalCompletionCoins)
                    .build();
            goalMilestoneRewardRepository.save(reward);

            updateUserCoinBalance(recipient, totalCompletionCoins);

            log.info("Individual goal {}: awarded 100% completion reward of {} coins to user {}",
                    goal.getId(), totalCompletionCoins, recipient.getId());

            return recipient.getId().equals(depositingUser.getId()) ? totalCompletionCoins : 0;
        }

        // Shared goal: distribute proportionally among all contributing members
        List<GoalContribution> contributions = goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId());

        Map<UUID, BigDecimal> userContributions = new LinkedHashMap<>();
        Map<UUID, User> userEntityMap = new HashMap<>();

        for (GoalContribution gc : contributions) {
            if (gc.getGoalMember() != null && gc.getGoalMember().getUser() != null) {
                BigDecimal amount = gc.getAmount() != null ? gc.getAmount() : BigDecimal.ZERO;
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    User u = gc.getGoalMember().getUser();
                    userContributions.merge(u.getId(), amount, BigDecimal::add);
                    userEntityMap.putIfAbsent(u.getId(), u);
                }
            }
        }

        if (userContributions.isEmpty()) {
            // Fallback if no contributions recorded yet: award to depositing user
            GoalMilestoneReward reward = GoalMilestoneReward.builder()
                    .goal(goal)
                    .user(depositingUser)
                    .milestone(GoalMilestone.MILESTONE_100)
                    .coinsEarned(totalCompletionCoins)
                    .build();
            goalMilestoneRewardRepository.save(reward);
            updateUserCoinBalance(depositingUser, totalCompletionCoins);
            return totalCompletionCoins;
        }

        if (userContributions.size() == 1) {
            // Only one member contributed -> receives 100% of completion reward
            UUID singleUserId = userContributions.keySet().iterator().next();
            User singleUser = userEntityMap.get(singleUserId);

            GoalMilestoneReward reward = GoalMilestoneReward.builder()
                    .goal(goal)
                    .user(singleUser)
                    .milestone(GoalMilestone.MILESTONE_100)
                    .coinsEarned(totalCompletionCoins)
                    .build();
            goalMilestoneRewardRepository.save(reward);
            updateUserCoinBalance(singleUser, totalCompletionCoins);

            return singleUserId.equals(depositingUser.getId()) ? totalCompletionCoins : 0;
        }

        // Multiple contributing members: Largest Remainder Method (Hare-Niemeyer)
        BigDecimal totalContributionAmount = BigDecimal.ZERO;
        for (BigDecimal amt : userContributions.values()) {
            totalContributionAmount = totalContributionAmount.add(amt);
        }

        Map<UUID, Integer> memberAllocations = allocateCoinsLargestRemainder(
                userContributions, totalContributionAmount, totalCompletionCoins
        );

        int depositingUserCoins = 0;

        for (Map.Entry<UUID, Integer> entry : memberAllocations.entrySet()) {
            UUID userId = entry.getKey();
            int coins = entry.getValue();
            if (coins <= 0) {
                continue;
            }

            User memberUser = userEntityMap.get(userId);
            GoalMilestoneReward reward = GoalMilestoneReward.builder()
                    .goal(goal)
                    .user(memberUser)
                    .milestone(GoalMilestone.MILESTONE_100)
                    .coinsEarned(coins)
                    .build();
            goalMilestoneRewardRepository.save(reward);
            updateUserCoinBalance(memberUser, coins);

            if (userId.equals(depositingUser.getId())) {
                depositingUserCoins += coins;
            }

            log.info("Shared goal {}: awarded {} completion coins to user {}",
                    goal.getId(), coins, userId);
        }

        return depositingUserCoins;
    }

    /**
     * Deterministic Largest Remainder Method (Hare-Niemeyer).
     * Guarantees that sum(memberCompletionRewards) == totalCoins exactly.
     */
    private Map<UUID, Integer> allocateCoinsLargestRemainder(
            Map<UUID, BigDecimal> userContributions,
            BigDecimal totalContribution,
            int totalCoins
    ) {
        Map<UUID, Integer> floors = new LinkedHashMap<>();
        Map<UUID, BigDecimal> remainders = new LinkedHashMap<>();

        int sumFloors = 0;
        BigDecimal totalCoinsBd = BigDecimal.valueOf(totalCoins);

        for (Map.Entry<UUID, BigDecimal> entry : userContributions.entrySet()) {
            UUID userId = entry.getKey();
            BigDecimal userAmt = entry.getValue();

            // quota = (userAmt * totalCoins) / totalContribution
            BigDecimal quota = userAmt.multiply(totalCoinsBd).divide(totalContribution, 10, RoundingMode.HALF_UP);
            int floor = quota.intValue();
            BigDecimal remainder = quota.subtract(BigDecimal.valueOf(floor));

            floors.put(userId, floor);
            remainders.put(userId, remainder);
            sumFloors += floor;
        }

        int remainderCoins = totalCoins - sumFloors;

        // Sort user IDs by remainder descending, then contribution descending, then UUID string for determinism
        List<UUID> sortedUsers = new ArrayList<>(userContributions.keySet());
        sortedUsers.sort((u1, u2) -> {
            int cmpRem = remainders.get(u2).compareTo(remainders.get(u1));
            if (cmpRem != 0) return cmpRem;
            int cmpAmt = userContributions.get(u2).compareTo(userContributions.get(u1));
            if (cmpAmt != 0) return cmpAmt;
            return u1.toString().compareTo(u2.toString());
        });

        for (int i = 0; i < remainderCoins && i < sortedUsers.size(); i++) {
            UUID luckyUser = sortedUsers.get(i);
            floors.put(luckyUser, floors.get(luckyUser) + 1);
        }

        return floors;
    }

    private void updateUserCoinBalance(User user, int coins) {
        int currentBalance = user.getCoinBalance() != null ? user.getCoinBalance() : 0;
        user.setCoinBalance(currentBalance + coins);
        userRepository.save(user);
    }

    @Override
    public boolean isSharedGoal(Goal goal) {
        if (goal == null || goal.getId() == null) {
            return false;
        }

        long activeCount = goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE);
        if (activeCount > 1) {
            return true;
        }

        long totalMembers = goalMemberRepository.countByGoal(goal);
        if (totalMembers > 1) {
            return true;
        }

        List<GoalContribution> contributions = goalContributionRepository.findByGoalIdOrderByContributedAtAsc(goal.getId());
        if (contributions != null) {
            Set<UUID> distinctUserIds = new HashSet<>();
            for (GoalContribution c : contributions) {
                if (c.getGoalMember() != null && c.getGoalMember().getUser() != null) {
                    distinctUserIds.add(c.getGoalMember().getUser().getId());
                }
            }
            if (distinctUserIds.size() > 1) {
                return true;
            }
        }

        return false;
    }
}
