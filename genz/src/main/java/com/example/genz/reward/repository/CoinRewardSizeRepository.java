package com.example.genz.reward.repository;

import com.example.genz.reward.entity.CoinRewardSize;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CoinRewardSizeRepository extends JpaRepository<CoinRewardSize, UUID> {

    List<CoinRewardSize> findByCoinRewardId(UUID coinRewardId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CoinRewardSize s WHERE s.id = :id")
    Optional<CoinRewardSize> findByIdForUpdate(@Param("id") UUID id);

    Optional<CoinRewardSize> findByIdAndCoinRewardId(UUID id, UUID coinRewardId);
}
