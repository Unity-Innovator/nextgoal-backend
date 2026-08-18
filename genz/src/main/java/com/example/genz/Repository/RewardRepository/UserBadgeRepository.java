package com.example.genz.repository.RewardRepository;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.Badge;
import com.example.genz.entity.Reward.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, UUID> {
    boolean existsByUserAndBadge(User user, Badge badge);
    Optional<UserBadge> findByUserAndBadge(User user, Badge badge);
    List<UserBadge> findByUserOrderByEarnedAtDesc(User user);
    long countByUser(User user);

    @Query("SELECT ub.badge.id FROM UserBadge ub WHERE ub.user = :user")
    List<UUID> findUnlockedBadgeIdsByUser(@Param("user") User user);
}
