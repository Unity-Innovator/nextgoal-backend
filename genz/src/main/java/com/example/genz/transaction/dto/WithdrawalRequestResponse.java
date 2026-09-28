package com.example.genz.transaction.dto;

import com.example.genz.transaction.enums.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalRequestResponse {
    private UUID id;
    private UUID goalId;
    private String goalTitle;
    private Integer memberCount;
    private BigDecimal targetSavings;
    private String managedBy;
    private String requesterName;
    private String requesterContribution;
    private BigDecimal requesterContributionAmount;
    private String requesterAvatar;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime createdAt;
    private Integer expireHours;
    private WithdrawalStatus status;
    private LocalDateTime resolvedAt;

    private Integer requiredApprovals;
    private Integer approvedCount;
    private Integer rejectedCount;
    private Integer pendingCount;

    private Boolean isRequester;
    private Boolean canApprove;
    private Boolean canConfirm;
    private Boolean canCancel;

    private List<WithdrawalMemberResponse> members;
}
