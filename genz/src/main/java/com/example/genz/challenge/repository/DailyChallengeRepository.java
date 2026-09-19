package com.example.genz.challenge.repository;

import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.enums.ChallengeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {
    List<DailyChallenge> findByIsActiveTrue();

    List<DailyChallenge> findByChallengeTypeAndIsActiveTrue(ChallengeType challengeType);
}
