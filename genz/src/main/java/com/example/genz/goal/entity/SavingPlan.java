package com.example.genz.goal.entity;

import com.example.genz.goal.enums.ContributionFrequency;
import com.example.genz.goal.enums.SavingPlanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saving_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SavingPlan {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "plan_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false, unique = true)
    private Goal goal;

    @Column(name = "contribution_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal contributionAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContributionFrequency frequency;

    @Column(name = "next_contribution_date", nullable = false)
    private LocalDate nextContributionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SavingPlanStatus status = SavingPlanStatus.ACTIVE;

    @Column(name = "contributed_this_period", nullable = false, precision = 19, scale = 4)
    private BigDecimal contributedThisPeriod = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
