package com.example.genz.Entity.Transaction;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.GoalMember;
import com.example.genz.Entity.Goal.WithdrawalRequest;
import com.example.genz.Entity.Wallet.GoalWallet;
import com.example.genz.Entity.Wallet.Wallet;
import com.example.genz.Enums.Currency;
import com.example.genz.Enums.GroupTransactionStatus;
import com.example.genz.Enums.GroupTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "group_wallet_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GroupWalletTransaction {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "group_wallet_transaction_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_wallet_id", nullable = false)
    private GoalWallet groupWallet;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_member_id", nullable = false)
    private GoalMember goalMember;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private Wallet paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_request_id")
    private WithdrawalRequest withdrawalRequest;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private GroupTransactionType transactionType;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @NotNull
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    @NotNull
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupTransactionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
