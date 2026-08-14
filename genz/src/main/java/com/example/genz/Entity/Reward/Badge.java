package com.example.genz.entity.Reward;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import com.example.genz.enums.BadgeRequirementType;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Badge {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "badge_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false)
    private BadgeRequirementType requirementType;

    @Column(name = "requirement_value")
    private Integer requirementValue;

    @OneToMany(mappedBy = "badge", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserBadge> userBadges;
}
