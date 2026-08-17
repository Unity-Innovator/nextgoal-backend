package com.example.genz.Repository.RewardRepository;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Reward.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, UUID> {
    Optional<UserReward> findByUser(User user);
    Optional<UserReward> findByUserId(UUID userId);
}
