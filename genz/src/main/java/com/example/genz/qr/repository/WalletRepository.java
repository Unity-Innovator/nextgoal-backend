package com.example.genz.qr.repository;

import com.example.genz.qr.entity.Wallet;
import com.example.genz.qr.enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.genz.auth.entity.User;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUsersAndCurrency(User user, Currency currency);
}
