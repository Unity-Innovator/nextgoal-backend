package com.example.genz.Entity.Goal;

import com.example.genz.Enums.Currency;
import com.example.genz.Enums.GoalStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Goal {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "goal_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @NotNull
    @Column(name = "target_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal targetAmount;

    @NotNull
    @Column(name = "current_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal currentAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @NotNull
    private LocalDate deadline;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GoalStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalContribution> contributions;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoalMember> members;

    @OneToMany(mappedBy = "goal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WithdrawalRequest> withdrawalRequests;
}
