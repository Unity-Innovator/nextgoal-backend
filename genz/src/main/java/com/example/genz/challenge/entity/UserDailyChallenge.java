package com.example.genz.challenge.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.enums.ChallengeStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_daily_challenges", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_daily_challenge", columnNames = {"user_id", "challenge_date"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDailyChallenge {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_daily_challenge_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private DailyChallenge challenge;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChallengeStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "challenge_date", nullable = false)
    private LocalDate challengeDate;
}
