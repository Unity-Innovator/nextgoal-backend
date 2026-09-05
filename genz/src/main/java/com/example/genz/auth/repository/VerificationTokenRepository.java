package com.example.genz.auth.repository;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {
    Optional<VerificationToken> findByTokenHash(String tokenHash);
    List<VerificationToken> findByPhoneNumberAndUsedFalse(String phoneNumber);
    List<VerificationToken> findByUserAndUsedFalse(User user);
}
