package com.example.genz.Entity.Wallet;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.example.genz.Entity.Goal.WithdrawalRequest;
import com.example.genz.Entity.Transaction.GroupWalletTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "goal_wallets")
public class GoalWallet {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "goal_number", nullable = false, unique = true)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WithdrawalRequest> withdrawalRequests;

    @OneToMany(mappedBy = "groupWallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupWalletTransaction> transactions;
}
