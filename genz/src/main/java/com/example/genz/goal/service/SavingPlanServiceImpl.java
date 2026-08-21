package com.example.genz.goal.service;

import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.entity.SavingPlan;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.goal.enums.ContributionFrequency;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.SavingPlanStatus;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.goal.repository.SavingPlanRepository;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.common.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SavingPlanServiceImpl implements SavingPlanService {

    private final SavingPlanRepository savingPlanRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public int processDueContributions() {
        LocalDate today = LocalDate.now();
        List<SavingPlan> duePlans = savingPlanRepository
                .findByNextContributionDateLessThanEqualAndGoal_Status(today, GoalStatus.ACTIVE);

        int checkedCount = 0;
        for (SavingPlan plan : duePlans) {
            // Check if contribution plan is active and has passed due date without being fulfilled
            if (plan.getStatus() == SavingPlanStatus.ACTIVE) {
                // If today is strictly past the due date and contributions are unfulfilled, it's overdue
                checkedCount++;
            }
        }
        return checkedCount;
    }

    private void advanceNextContributionDate(SavingPlan plan) {
        plan.setNextContributionDate(plan.getFrequency() == ContributionFrequency.WEEKLY
                ? plan.getNextContributionDate().plusWeeks(1)
                : plan.getNextContributionDate().plusMonths(1));
    }
}
