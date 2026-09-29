package com.example.genz.transaction.service;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.exception.BadRequestException;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;
import com.example.genz.goal.enums.GoalStatus;
import com.example.genz.goal.enums.MemberStatus;
import com.example.genz.goal.repository.GoalContributionRepository;
import com.example.genz.goal.repository.GoalMemberRepository;
import com.example.genz.goal.repository.GoalRepository;
import com.example.genz.transaction.dto.CreateWithdrawalRequest;
import com.example.genz.transaction.dto.WithdrawalMemberResponse;
import com.example.genz.transaction.dto.WithdrawalRequestResponse;
import com.example.genz.transaction.entity.GroupWalletTransaction;
import com.example.genz.transaction.entity.WalletTransaction;
import com.example.genz.transaction.entity.WithdrawalApproval;
import com.example.genz.transaction.entity.WithdrawalRequest;
import com.example.genz.transaction.enums.ApprovalDecision;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.GroupTransactionStatus;
import com.example.genz.transaction.enums.GroupTransactionType;
import com.example.genz.transaction.enums.TransactionStatus;
import com.example.genz.transaction.enums.TransactionType;
import com.example.genz.transaction.enums.WithdrawalStatus;
import com.example.genz.transaction.repository.GroupWalletTransactionRepository;
import com.example.genz.transaction.repository.WalletTransactionRepository;
import com.example.genz.transaction.repository.WithdrawalApprovalRepository;
import com.example.genz.transaction.repository.WithdrawalRequestRepository;
import com.example.genz.wallet.entity.GrouplWallet;
import com.example.genz.wallet.repository.GoalWalletRepository;
import com.example.genz.goal.enums.MemberRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRequestServiceImpl implements WithdrawalRequestService {

    private final UserRepository userRepository;
    private final GoalRepository goalRepository;
    private final GoalMemberRepository goalMemberRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final GoalContributionRepository goalContributionRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final WithdrawalApprovalRepository withdrawalApprovalRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final GroupWalletTransactionRepository groupWalletTransactionRepository;

    @Override
    @Transactional
    public WithdrawalRequestResponse createRequest(UUID goalId, CreateWithdrawalRequest request, String phoneNumber) {
        User requester = getUserByPhone(phoneNumber);

        UUID effectiveGoalId = goalId != null ? goalId : request.getGoalId();
        if (effectiveGoalId == null) {
            throw new BadRequestException("Goal ID is required");
        }

        Goal goal = goalRepository.findById(effectiveGoalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        if (goal.getStatus() == GoalStatus.CANCELLED || goal.getStatus() == GoalStatus.ARCHIVED) {
            throw new BadRequestException("Cannot create withdrawal request on an inactive goal");
        }

        GoalMember requesterMember = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), requester.getId())
                .orElseGet(() -> {
                    boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(requester.getId());
                    if (isOwner) {
                        GoalMember newOwnerMember = new GoalMember();
                        newOwnerMember.setGoal(goal);
                        newOwnerMember.setUser(requester);
                        newOwnerMember.setRole(MemberRole.OWNER);
                        newOwnerMember.setStatus(MemberStatus.ACTIVE);
                        return goalMemberRepository.save(newOwnerMember);
                    }
                    throw new AccessDeniedException("User is not a member of this goal");
                });

        if (requesterMember.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Only active members can request a withdrawal");
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Withdrawal amount must be greater than zero");
        }

        BigDecimal currentBalance = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        if (amount.compareTo(currentBalance) > 0) {
            throw new BadRequestException("Withdrawal amount cannot exceed current goal balance of $" + currentBalance);
        }

        boolean hasActiveRequest = withdrawalRequestRepository.existsByGoalIdAndStatusIn(
                goal.getId(),
                List.of(WithdrawalStatus.PENDING, WithdrawalStatus.APPROVED)
        );
        if (hasActiveRequest) {
            throw new BadRequestException("An active withdrawal request already exists for this goal");
        }

        GrouplWallet wallet = goalWalletRepository.findByGoal(goal)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal wallet not found for goal: " + goal.getGoalName()));

        List<GoalMember> otherActiveMembers = goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE)
                .stream()
                .filter(m -> !m.getUser().getId().equals(requester.getId()))
                .toList();

        WithdrawalRequest wr = new WithdrawalRequest();
        wr.setGoal(goal);
        wr.setWallet(wallet);
        wr.setRequestedBy(requester);
        wr.setAmount(amount);
        wr.setReason(request.getReason() != null ? request.getReason().trim() : "");
        wr.setCreatedAt(LocalDateTime.now());

        if (otherActiveMembers.isEmpty()) {
            wr.setStatus(WithdrawalStatus.APPROVED);
            wr.setResolvedAt(LocalDateTime.now());
        } else {
            wr.setStatus(WithdrawalStatus.PENDING);
        }

        wr = withdrawalRequestRepository.save(wr);

        for (GoalMember member : otherActiveMembers) {
            WithdrawalApproval approval = new WithdrawalApproval();
            approval.setWithdrawalRequest(wr);
            approval.setUser(member.getUser());
            approval.setDecision(ApprovalDecision.PENDING);
            approval.setDecidedAt(LocalDateTime.now());
            withdrawalApprovalRepository.save(approval);
        }

        return toResponse(wr, requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithdrawalRequestResponse> getRequests(UUID goalId, WithdrawalStatus status, String phoneNumber) {
        User currentUser = getUserByPhone(phoneNumber);

        List<WithdrawalRequest> requests;
        if (goalId != null) {
            Goal goal = goalRepository.findById(goalId)
                    .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

            boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(currentUser.getId());
            boolean isMember = goalMemberRepository.existsByGoalIdAndUserIdAndStatus(goal.getId(), currentUser.getId(), MemberStatus.ACTIVE);
            if (!isOwner && !isMember) {
                throw new AccessDeniedException("Access denied: user is not an active member or owner of this goal");
            }

            if (status != null) {
                requests = withdrawalRequestRepository.findByGoalIdAndStatusOrderByCreatedAtDesc(goalId, status);
            } else {
                requests = withdrawalRequestRepository.findByGoalIdOrderByCreatedAtDesc(goalId);
            }
        } else {
            if (status != null) {
                requests = withdrawalRequestRepository.findAccessibleRequestsByStatus(currentUser.getId(), status);
            } else {
                requests = withdrawalRequestRepository.findAccessibleRequests(currentUser.getId());
            }
        }

        return requests.stream()
                .map(r -> toResponse(r, currentUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawalRequestResponse getRequestById(UUID requestId, String phoneNumber) {
        User currentUser = getUserByPhone(phoneNumber);
        WithdrawalRequest wr = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNoteFoundException("Withdrawal request not found with ID: " + requestId));

        boolean isOwner = wr.getGoal().getOwner() != null && wr.getGoal().getOwner().getId().equals(currentUser.getId());
        boolean isMember = goalMemberRepository.existsByGoalIdAndUserIdAndStatus(wr.getGoal().getId(), currentUser.getId(), MemberStatus.ACTIVE);
        if (!isOwner && !isMember) {
            throw new AccessDeniedException("Access denied: user is not an active member or owner of this goal");
        }

        return toResponse(wr, currentUser);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponse approveRequest(UUID requestId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        WithdrawalRequest wr = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNoteFoundException("Withdrawal request not found"));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Withdrawal request is not pending approval. Current status: " + wr.getStatus());
        }

        if (wr.getRequestedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Requester cannot approve their own withdrawal request");
        }

        GoalMember member = goalMemberRepository.findByGoalIdAndUserId(wr.getGoal().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this goal"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Only active members can approve withdrawal requests");
        }

        WithdrawalApproval approval = withdrawalApprovalRepository.findByWithdrawalRequestIdAndUserId(wr.getId(), user.getId())
                .orElseThrow(() -> new BadRequestException("User is not a designated approver for this request"));

        if (approval.getDecision() != ApprovalDecision.PENDING) {
            throw new BadRequestException("User has already submitted a decision for this request");
        }

        approval.setDecision(ApprovalDecision.APPROVED);
        approval.setDecidedAt(LocalDateTime.now());
        withdrawalApprovalRepository.save(approval);

        recalculateRequestStatus(wr);

        return toResponse(wr, user);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponse rejectRequest(UUID requestId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        WithdrawalRequest wr = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNoteFoundException("Withdrawal request not found"));

        if (wr.getStatus() != WithdrawalStatus.PENDING) {
            throw new BadRequestException("Withdrawal request is not pending approval. Current status: " + wr.getStatus());
        }

        if (wr.getRequestedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Requester cannot reject their own withdrawal request");
        }

        GoalMember member = goalMemberRepository.findByGoalIdAndUserId(wr.getGoal().getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a member of this goal"));

        if (member.getStatus() != MemberStatus.ACTIVE) {
            throw new AccessDeniedException("Only active members can reject withdrawal requests");
        }

        WithdrawalApproval approval = withdrawalApprovalRepository.findByWithdrawalRequestIdAndUserId(wr.getId(), user.getId())
                .orElseThrow(() -> new BadRequestException("User is not a designated approver for this request"));

        if (approval.getDecision() != ApprovalDecision.PENDING) {
            throw new BadRequestException("User has already submitted a decision for this request");
        }

        approval.setDecision(ApprovalDecision.REJECTED);
        approval.setDecidedAt(LocalDateTime.now());
        withdrawalApprovalRepository.save(approval);

        wr.setStatus(WithdrawalStatus.REJECTED);
        wr.setResolvedAt(LocalDateTime.now());
        wr = withdrawalRequestRepository.save(wr);

        return toResponse(wr, user);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponse cancelRequest(UUID requestId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        WithdrawalRequest wr = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNoteFoundException("Withdrawal request not found"));

        if (!wr.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the requester can cancel the withdrawal request");
        }

        if (wr.getStatus() != WithdrawalStatus.PENDING && wr.getStatus() != WithdrawalStatus.APPROVED) {
            throw new BadRequestException("Cannot cancel withdrawal request with status: " + wr.getStatus());
        }

        wr.setStatus(WithdrawalStatus.CANCELLED);
        wr.setResolvedAt(LocalDateTime.now());
        wr = withdrawalRequestRepository.save(wr);

        return toResponse(wr, user);
    }

    @Override
    @Transactional
    public WithdrawalRequestResponse confirmWithdrawal(UUID requestId, String phoneNumber) {
        User user = getUserByPhone(phoneNumber);
        WithdrawalRequest wr = withdrawalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNoteFoundException("Withdrawal request not found"));

        if (!wr.getRequestedBy().getId().equals(user.getId())) {
            throw new AccessDeniedException("Only the original requester can confirm the withdrawal");
        }

        if (wr.getStatus() == WithdrawalStatus.COMPLETED) {
            throw new BadRequestException("Withdrawal request has already been completed");
        }

        if (wr.getStatus() != WithdrawalStatus.APPROVED) {
            throw new BadRequestException("Withdrawal request cannot be confirmed in status: " + wr.getStatus());
        }

        Goal goal = goalRepository.findByIdForUpdate(wr.getGoal().getId())
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        BigDecimal currentGoalBalance = goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO;
        BigDecimal withdrawalAmount = wr.getAmount();

        if (currentGoalBalance.compareTo(withdrawalAmount) < 0) {
            throw new BadRequestException("Insufficient goal balance for withdrawal. Current balance: $" + currentGoalBalance + ", requested: $" + withdrawalAmount);
        }

        BigDecimal newGoalAmount = currentGoalBalance.subtract(withdrawalAmount);
        goal.setCurrentAmount(newGoalAmount);

        BigDecimal targetAmount = goal.getTargetAmount() != null ? goal.getTargetAmount() : BigDecimal.ZERO;
        BigDecimal remainingAmount = targetAmount.subtract(newGoalAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }
        goal.setRemaining(remainingAmount);
        goalRepository.save(goal);

        GrouplWallet wallet = goalWalletRepository.findByGoal(goal)
                .orElseGet(() -> {
                    GrouplWallet newWallet = new GrouplWallet();
                    newWallet.setGoal(goal);
                    newWallet.setUser(goal.getOwner() != null ? goal.getOwner() : user);
                    newWallet.setBalance(goal.getCurrentAmount() != null ? goal.getCurrentAmount() : BigDecimal.ZERO);
                    newWallet.setCurrency(Currency.USD);
                    return goalWalletRepository.save(newWallet);
                });

        BigDecimal currentWalletBalance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal newWalletBalance = currentWalletBalance.subtract(withdrawalAmount);
        wallet.setBalance(newWalletBalance);
        goalWalletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setUser(user);
        transaction.setAmount(withdrawalAmount);
        transaction.setType(TransactionType.WITHDRAWAL);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Withdrawal from goal: " + goal.getGoalName());
        transaction.setCreatedAt(LocalDateTime.now());
        walletTransactionRepository.save(transaction);

        // Locate requester's GoalMember record with fallback to owner/creator
        GoalMember requesterMember = goalMemberRepository.findByGoalIdAndUserId(goal.getId(), user.getId())
                .orElseGet(() -> {
                    return goalMemberRepository.findByGoal(goal).stream()
                            .filter(m -> m.getUser() != null && m.getUser().getId().equals(user.getId()))
                            .findFirst()
                            .orElseGet(() -> {
                                boolean isOwner = goal.getOwner() != null && goal.getOwner().getId().equals(user.getId());
                                if (isOwner) {
                                    GoalMember newOwnerMember = new GoalMember();
                                    newOwnerMember.setGoal(goal);
                                    newOwnerMember.setUser(user);
                                    newOwnerMember.setRole(MemberRole.OWNER);
                                    newOwnerMember.setStatus(MemberStatus.ACTIVE);
                                    return goalMemberRepository.save(newOwnerMember);
                                }
                                return null;
                            });
                });

        if (requesterMember != null) {
            GroupWalletTransaction groupTx = new GroupWalletTransaction();
            groupTx.setGroupWallet(wallet);
            groupTx.setGoalMember(requesterMember);
            groupTx.setWithdrawalRequest(wr);
            groupTx.setTransactionType(GroupTransactionType.WITHDRAWAL);
            groupTx.setAmount(withdrawalAmount);
            groupTx.setCurrency(wallet.getCurrency() != null ? wallet.getCurrency() : Currency.USD);
            groupTx.setBalanceBefore(currentWalletBalance);
            groupTx.setBalanceAfter(newWalletBalance);
            groupTx.setStatus(GroupTransactionStatus.COMPLETED);
            groupTx.setCreatedAt(LocalDateTime.now());
            groupWalletTransactionRepository.save(groupTx);
        } else {
            log.warn("Could not find or create GoalMember for user {} in goal {}. Skipping group wallet member transaction.",
                    user.getId(), goal.getId());
        }

        wr.setStatus(WithdrawalStatus.COMPLETED);
        wr.setResolvedAt(LocalDateTime.now());
        wr = withdrawalRequestRepository.save(wr);

        return toResponse(wr, user);
    }

    private void recalculateRequestStatus(WithdrawalRequest wr) {
        List<GoalMember> currentActiveMembers = goalMemberRepository.findByGoalAndStatusWithUser(wr.getGoal(), MemberStatus.ACTIVE)
                .stream()
                .filter(m -> !m.getUser().getId().equals(wr.getRequestedBy().getId()))
                .toList();

        List<WithdrawalApproval> approvals = withdrawalApprovalRepository.findByWithdrawalRequestId(wr.getId());
        Map<UUID, ApprovalDecision> decisionMap = approvals.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), WithdrawalApproval::getDecision, (d1, d2) -> d1));

        boolean anyRejected = currentActiveMembers.stream()
                .anyMatch(m -> decisionMap.get(m.getUser().getId()) == ApprovalDecision.REJECTED);

        if (anyRejected) {
            wr.setStatus(WithdrawalStatus.REJECTED);
            wr.setResolvedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(wr);
            return;
        }

        boolean allApproved = currentActiveMembers.isEmpty() || currentActiveMembers.stream()
                .allMatch(m -> decisionMap.get(m.getUser().getId()) == ApprovalDecision.APPROVED);

        if (allApproved) {
            wr.setStatus(WithdrawalStatus.APPROVED);
            wr.setResolvedAt(LocalDateTime.now());
            withdrawalRequestRepository.save(wr);
        }
    }

    private WithdrawalRequestResponse toResponse(WithdrawalRequest wr, User currentUser) {
        Goal goal = wr.getGoal();
        long activeMemberCount = goalMemberRepository.countByGoalAndStatus(goal, MemberStatus.ACTIVE);
        String managedBy = (goal.getOwner() != null && goal.getOwner().getFullName() != null)
                ? goal.getOwner().getFullName()
                : "Group Admin";

        BigDecimal requesterContributionAmount = goalContributionRepository.sumAmountByGoalIdAndUserId(goal.getId(), wr.getRequestedBy().getId());
        if (requesterContributionAmount == null) {
            requesterContributionAmount = BigDecimal.ZERO;
        }
        String requesterContributionFormatted = String.format("$%,.2f", requesterContributionAmount);

        String requesterAvatar = (wr.getRequestedBy().getSettings() != null)
                ? wr.getRequestedBy().getSettings().getAvatarUrl()
                : null;

        List<GoalMember> activeMembers = goalMemberRepository.findByGoalAndStatusWithUser(goal, MemberStatus.ACTIVE);
        List<WithdrawalApproval> approvals = withdrawalApprovalRepository.findByWithdrawalRequestId(wr.getId());
        Map<UUID, WithdrawalApproval> approvalMap = approvals.stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), a -> a, (a1, a2) -> a1));

        List<WithdrawalMemberResponse> memberResponses = new ArrayList<>();
        int requiredApprovals = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (GoalMember m : activeMembers) {
            UUID memberUserId = m.getUser().getId();
            BigDecimal contrib = goalContributionRepository.sumAmountByGoalIdAndUserId(goal.getId(), memberUserId);
            if (contrib == null) contrib = BigDecimal.ZERO;
            String contribFormatted = String.format("$%,.2f", contrib);
            String avatar = (m.getUser().getSettings() != null) ? m.getUser().getSettings().getAvatarUrl() : null;

            if (memberUserId.equals(wr.getRequestedBy().getId())) {
                memberResponses.add(WithdrawalMemberResponse.builder()
                        .id(m.getId().toString())
                        .userId(memberUserId)
                        .name(m.getUser().getFullName())
                        .contribution(contribFormatted)
                        .contributionAmount(contrib)
                        .avatarUrl(avatar)
                        .status("Approved")
                        .decision(ApprovalDecision.APPROVED)
                        .decidedAt(wr.getCreatedAt())
                        .build());
            } else {
                requiredApprovals++;
                WithdrawalApproval app = approvalMap.get(memberUserId);
                ApprovalDecision dec = app != null ? app.getDecision() : ApprovalDecision.PENDING;
                LocalDateTime decidedAt = app != null ? app.getDecidedAt() : null;

                String displayStatus;
                if (dec == ApprovalDecision.APPROVED) {
                    approvedCount++;
                    displayStatus = "Approved";
                } else if (dec == ApprovalDecision.REJECTED) {
                    rejectedCount++;
                    displayStatus = "Rejected";
                } else {
                    displayStatus = "Pending";
                }

                memberResponses.add(WithdrawalMemberResponse.builder()
                        .id(m.getId().toString())
                        .userId(memberUserId)
                        .name(m.getUser().getFullName())
                        .contribution(contribFormatted)
                        .contributionAmount(contrib)
                        .avatarUrl(avatar)
                        .status(displayStatus)
                        .decision(dec)
                        .decidedAt(decidedAt)
                        .build());
            }
        }

        int pendingCount = Math.max(0, requiredApprovals - approvedCount - rejectedCount);

        boolean isRequester = currentUser != null && wr.getRequestedBy().getId().equals(currentUser.getId());
        boolean canConfirm = isRequester && wr.getStatus() == WithdrawalStatus.APPROVED;
        boolean canCancel = isRequester && (wr.getStatus() == WithdrawalStatus.PENDING || wr.getStatus() == WithdrawalStatus.APPROVED);

        WithdrawalApproval currentUserApproval = currentUser != null ? approvalMap.get(currentUser.getId()) : null;
        boolean canApprove = !isRequester && wr.getStatus() == WithdrawalStatus.PENDING &&
                currentUserApproval != null && currentUserApproval.getDecision() == ApprovalDecision.PENDING;

        return WithdrawalRequestResponse.builder()
                .id(wr.getId())
                .goalId(goal.getId())
                .goalTitle(goal.getGoalName())
                .memberCount((int) activeMemberCount)
                .targetSavings(goal.getTargetAmount())
                .managedBy(managedBy)
                .requesterName(wr.getRequestedBy().getFullName())
                .requesterContribution(requesterContributionFormatted)
                .requesterContributionAmount(requesterContributionAmount)
                .requesterAvatar(requesterAvatar)
                .amount(wr.getAmount())
                .reason(wr.getReason())
                .createdAt(wr.getCreatedAt())
                .expireHours(48)
                .status(wr.getStatus())
                .resolvedAt(wr.getResolvedAt())
                .requiredApprovals(requiredApprovals)
                .approvedCount(approvedCount)
                .rejectedCount(rejectedCount)
                .pendingCount(pendingCount)
                .isRequester(isRequester)
                .canApprove(canApprove)
                .canConfirm(canConfirm)
                .canCancel(canCancel)
                .members(memberResponses)
                .build();
    }

    private User getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone: " + phoneNumber));
    }
}
