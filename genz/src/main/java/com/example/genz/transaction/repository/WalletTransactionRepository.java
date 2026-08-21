package com.example.genz.transaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.transaction.entity.WalletTransaction;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
}
