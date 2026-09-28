package com.example.genz.goal.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import com.example.genz.auth.entity.User;
import com.example.genz.transaction.entity.GoalContribution;
import com.example.genz.goal.enums.MemberRole;
import com.example.genz.goal.enums.MemberStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "goal_members", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uk_goal_members_goal_user", columnNames = {"goal_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoalMember {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "goal_member_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberRole role; 

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MemberStatus status; 

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private LocalDateTime joinedAt;

    @OneToMany(mappedBy = "goalMember", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private java.util.List<GoalContribution> contributions;
}