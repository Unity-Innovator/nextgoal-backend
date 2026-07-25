package com.example.genz.Entity.QrCode;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
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
    @Column(name = "wallet_id", columnDefinition = "uuid",updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Balance is required")
    private BigDecimal balance;

    @NotBlank(message = "Goal number is required")
    @Column(name = "goal_number")
    private String goalNumber;

    @NotNull(message = "Creator ID is required")
    @Column(name = "creator_id")
    private UUID creatorId;

    @NotNull(message = "Goal ID is required")
    @Column(name = "goal_id")
    private UUID goalId;
}
