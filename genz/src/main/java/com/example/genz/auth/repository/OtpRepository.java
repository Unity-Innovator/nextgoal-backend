package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.auth.entity.Otp;
import com.example.genz.auth.enums.OtpPurpose;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findFirstByPhoneNumberAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String phoneNumber,
            OtpPurpose purpose
    );

    long countByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber,
            OtpPurpose purpose,
            LocalDateTime after
    );
}
