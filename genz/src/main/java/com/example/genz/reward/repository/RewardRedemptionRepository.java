package com.example.genz.reward.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.reward.entity.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, UUID> {

    List<RewardRedemption> findByUserOrderByCreatedAtDesc(User user);

    boolean existsByReferenceNumber(String referenceNumber);
}
