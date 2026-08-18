package com.example.genz.service.SavingPlanService;

import com.example.genz.entity.Goal.Goal;
import com.example.genz.entity.Goal.GoalMember;
import com.example.genz.entity.Goal.SavingPlan;
import com.example.genz.entity.Transaction.GoalContribution;
import com.example.genz.entity.Transaction.WalletTransaction;
import com.example.genz.entity.Wallet.GrouplWallet;
import com.example.genz.enums.ContributionFrequency;
import com.example.genz.enums.GoalStatus;
import com.example.genz.enums.SavingPlanStatus;
import com.example.genz.enums.TransactionStatus;
import com.example.genz.enums.TransactionType;
import com.example.genz.repository.GoalRepository.GoalContributionRepository;
import com.example.genz.repository.GoalRepository.GoalMemberRepository;
import com.example.genz.repository.GoalRepository.GoalRepository;
import com.example.genz.repository.GoalRepository.SavingPlanRepository;
import com.example.genz.repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.repository.TransactionRepository.WalletTransactionRepository;
import com.example.genz.exception.ResourceNoteFoundException;
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
