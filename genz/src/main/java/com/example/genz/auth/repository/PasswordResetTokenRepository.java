package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.genz.auth.entity.PasswordResetToken;
import com.example.genz.auth.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}
