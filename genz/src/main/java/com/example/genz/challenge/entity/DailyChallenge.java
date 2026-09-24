package com.example.genz.challenge.entity;

import com.example.genz.challenge.enums.ChallengeRuleType;
import com.example.genz.challenge.enums.ChallengeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "daily_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChallenge {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "challenge_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false)
    private ChallengeType challengeType;

    @Column(name = "target_value", precision = 19, scale = 4)
    private BigDecimal targetValue;

    @Builder.Default
    @Column(name = "reward_coins")
    private Integer rewardCoins = 0;

    @Embedded
    private ChallengeRule rule;

    @Column(name = "xp_reward")
    private Integer xpReward;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserDailyChallenge> userDailyChallenges;

    public boolean isAutomaticallyVerifiable() {
        if (rule != null && rule.getRuleType() == ChallengeRuleType.MANUAL_CONFIRM) {
            return false;
        }
        if (challengeType == ChallengeType.MANUAL || challengeType == ChallengeType.TIP) {
            return false;
        }
        return true;
    }
}
