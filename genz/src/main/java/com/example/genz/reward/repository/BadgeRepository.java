package com.example.genz.reward.repository;

import com.example.genz.reward.entity.Badge;
import com.example.genz.reward.enums.BadgeCategory;
import com.example.genz.reward.enums.BadgeRequirementType;
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
