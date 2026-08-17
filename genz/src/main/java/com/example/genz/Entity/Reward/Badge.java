package com.example.genz.Entity.Reward;

import com.example.genz.Enums.BadgeCategory;
import com.example.genz.Enums.BadgeRarity;
import com.example.genz.Enums.BadgeRequirementType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Column(name = "category", nullable = false)
    private BadgeCategory category;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", nullable = false)
    private BadgeRequirementType requirementType;

    @Column(name = "requirement_value")
    private Integer requirementValue;

    @Column(name = "xp_reward", nullable = false)
    private Integer xpReward;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private BadgeRarity rarity;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "badge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserBadge> userBadges = new ArrayList<>();
}
