package com.example.genz.Repository.TransactionRepository;

import com.example.genz.Entity.Transaction.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
}
