package com.example.genz.repository.TransactionRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.genz.entity.Transaction.WalletTransaction;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
}
