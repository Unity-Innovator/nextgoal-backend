package com.example.genz.Repository.DailyChallengeRepository;

import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.DailyChallenge.UserDailyChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDailyChallengeRepository extends JpaRepository<UserDailyChallenge, UUID> {
    Optional<UserDailyChallenge> findByUserAndChallengeDate(User user, LocalDate challengeDate);
    boolean existsByUserAndChallengeDate(User user, LocalDate challengeDate);
    Optional<UserDailyChallenge> findByIdAndUser(UUID id, User user);
}
