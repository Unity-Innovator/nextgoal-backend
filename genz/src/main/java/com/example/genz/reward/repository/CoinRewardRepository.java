package com.example.genz.reward.repository;

import com.example.genz.reward.entity.CoinReward;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoinRewardRepository extends JpaRepository<CoinReward, UUID> {

    List<CoinReward> findByIsActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CoinReward r WHERE r.id = :id")
    Optional<CoinReward> findByIdForUpdate(@Param("id") UUID id);
}
