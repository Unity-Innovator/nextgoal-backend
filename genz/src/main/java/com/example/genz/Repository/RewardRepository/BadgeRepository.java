package com.example.genz.Repository.RewardRepository;

import com.example.genz.Entity.Reward.Badge;
import com.example.genz.Enums.BadgeCategory;
import com.example.genz.Enums.BadgeRequirementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, UUID> {
    List<Badge> findByActiveTrue();
    List<Badge> findByRequirementTypeAndActiveTrue(BadgeRequirementType requirementType);
    List<Badge> findByCategoryAndActiveTrue(BadgeCategory category);
    Optional<Badge> findByName(String name);
}
