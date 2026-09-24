package com.example.genz.reward.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, UUID> {
    Optional<UserReward> findByUser(User user);
    Optional<UserReward> findByUserId(UUID userId);
}
