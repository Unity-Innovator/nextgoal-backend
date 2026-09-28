package com.example.genz.transaction.entity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.example.genz.goal.entity.Goal;
import com.example.genz.goal.entity.GoalMember;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_contributions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalContribution {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "contribution_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne
    @JoinColumn(name = "goal_member_id", nullable = false)
    private GoalMember goalMember;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String note;

    @Column(name = "contributed_at", nullable = false, updatable = false)
    private LocalDateTime contributedAt;

    @PrePersist
    protected void onCreate() {
        if (contributedAt == null) {
            contributedAt = LocalDateTime.now();
        }
    }
}
