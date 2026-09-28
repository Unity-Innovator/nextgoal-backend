package com.example.genz.transaction.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.transaction.enums.ApprovalDecision;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "withdrawal_approvals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_withdrawal_approvals_request_user", columnNames = {"request_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalApproval {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "approval_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private WithdrawalRequest withdrawalRequest;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalDecision decision;

    @CreationTimestamp
    @Column(name = "decided_at", nullable = false)
    private LocalDateTime decidedAt;
}
