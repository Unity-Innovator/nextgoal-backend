package com.example.genz.transaction.dto;

import com.example.genz.transaction.enums.ApprovalDecision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawalMemberResponse {
    private String id;
    private UUID userId;
    private String name;
    private String contribution;
    private BigDecimal contributionAmount;
    private String avatarUrl;
    private String status; // 'Pending', 'Approved', 'Rejected'
    private ApprovalDecision decision;
    private LocalDateTime decidedAt;
}
