package com.example.genz.reward.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.enums.RewardRedemptionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reward_redemptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardRedemption {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "reference_number", nullable = false, unique = true, length = 60)
    private String referenceNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_reward_id", nullable = false)
    private CoinReward coinReward;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coin_reward_size_id")
    private CoinRewardSize coinRewardSize;

    @NotNull
    @Column(name = "item_name", nullable = false, length = 100)
    private String itemName;

    @Column(name = "selected_size", length = 20)
    private String selectedSize;

    @NotNull
    @Column(name = "coins_used", nullable = false)
    private int coinsUsed;

    @NotNull
    @Column(name = "phone_number", nullable = false, length = 30)
    private String phoneNumber;

    @NotNull
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RewardRedemptionStatus status;

    @NotNull
    @Column(name = "estimated_delivery_from", nullable = false)
    private LocalDate estimatedDeliveryFrom;

    @NotNull
    @Column(name = "estimated_delivery_to", nullable = false)
    private LocalDate estimatedDeliveryTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
