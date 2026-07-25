package com.example.genz.Repository.TransactionRepository;

import com.example.genz.Entity.Transaction.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
}
