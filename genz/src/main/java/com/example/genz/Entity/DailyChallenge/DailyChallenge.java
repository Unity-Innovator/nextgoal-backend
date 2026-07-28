package com.example.genz.Entity.DailyChallenge;
import com.example.genz.Enums.ChallengeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "daily_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "xp_reward")
    private Integer xpReward;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserDailyChallenge> userDailyChallenges;
}
