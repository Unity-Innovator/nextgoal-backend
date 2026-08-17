package com.example.genz.Repository.DailyChallengeRepository;

import com.example.genz.Entity.DailyChallenge.DailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DailyChallengeRepository extends JpaRepository<DailyChallenge, UUID> {
    List<DailyChallenge> findByIsActiveTrue();
}
