package com.example.genz.Entity.Reward;

import com.example.genz.Entity.Auth.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserReward {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "reward_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "total_xp")
    private Integer totalXp;

    private Integer level;

    @Column(name = "current_streak")
    private Integer currentStreak;

    @Column(name = "longest_streak")
    private Integer longestStreak;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
