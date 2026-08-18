package com.example.genz.repository.RewardRepository;

import com.example.genz.entity.Auth.User;
import com.example.genz.entity.Reward.UserReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRewardRepository extends JpaRepository<UserReward, UUID> {
    Optional<UserReward> findByUser(User user);
    Optional<UserReward> findByUserId(UUID userId);
}
