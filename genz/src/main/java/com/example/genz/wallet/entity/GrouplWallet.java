package com.example.genz.wallet.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.goal.entity.Goal;
import com.example.genz.transaction.entity.GroupWalletTransaction;
import com.example.genz.transaction.entity.WithdrawalRequest;
import com.example.genz.transaction.enums.Currency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "group_wallets")
public class GrouplWallet {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false, unique = true)
    private Goal goal;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WithdrawalRequest> withdrawalRequests;

    @OneToMany(mappedBy = "groupWallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupWalletTransaction> transactions;
}
