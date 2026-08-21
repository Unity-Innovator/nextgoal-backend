package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.auth.entity.Otp;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findFirstByPhoneNumberAndUsedFalseOrderByCreatedAtDesc(String phoneNumber);

    long countByPhoneNumberAndCreatedAtAfter(String phoneNumber, LocalDateTime after);
}