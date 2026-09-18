package com.example.genz.reward.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.genz.reward.enums.RewardSize;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.example.genz.reward.entity.CoinReward;

@Entity
@Table(name = "coin_reward_sizes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class CoinRewardSize {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coin_reward_id", nullable = false)
    private CoinReward coinReward;

    @Enumerated(EnumType.STRING)
    @Column(name = "size", nullable = false, length = 10)
    private RewardSize size;

    @Column(name = "stock", nullable = false)
    private int stock;
}