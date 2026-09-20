package com.example.genz.challenge.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.challenge.entity.DailyChallenge;
import com.example.genz.challenge.entity.UserDailyChallenge;
import com.example.genz.challenge.enums.ChallengeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDailyChallengeRepository extends JpaRepository<UserDailyChallenge, UUID> {

    Optional<UserDailyChallenge> findByUserAndChallengeDate(User user, LocalDate challengeDate);

    boolean existsByUserAndChallengeDate(User user, LocalDate challengeDate);

    Optional<UserDailyChallenge> findByIdAndUser(UUID id, User user);

    List<UserDailyChallenge> findAllByUserAndChallengeDate(User user, LocalDate challengeDate);

    Optional<UserDailyChallenge> findByUserAndChallengeAndChallengeDate(User user, DailyChallenge challenge, LocalDate challengeDate);

    boolean existsByUserAndChallengeAndChallengeDateAndStatus(User user, DailyChallenge challenge, LocalDate challengeDate, ChallengeStatus status);

    boolean existsByUserAndChallengeAndChallengeDate(User user, DailyChallenge challenge, LocalDate challengeDate);

    @Query("SELECT DISTINCT udc.challengeDate FROM UserDailyChallenge udc WHERE udc.user.id = :userId AND udc.status = 'COMPLETED' ORDER BY udc.challengeDate DESC")
    List<LocalDate> findDistinctCompletedDatesByUserId(@Param("userId") UUID userId);
}
