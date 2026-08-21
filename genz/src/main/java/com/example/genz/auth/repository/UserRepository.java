package com.example.genz.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhoneNumber(String phoneNumber);
    boolean existsByPhoneNumber(String phoneNumber);
}
