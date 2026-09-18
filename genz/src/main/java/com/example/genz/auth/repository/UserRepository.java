package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber")
    Optional<User> findByPhoneNumberForUpdate(@Param("phoneNumber") String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);
}
