package com.example.genz.Repository.QrCodeRepository;

import com.example.genz.Entity.QrCode.Wallet;
import com.example.genz.Enums.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.genz.Entity.OTP.User;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    Optional<Wallet> findByUsersAndCurrency(User user, Currency currency);
    Wallet findByAccountNumber(String senderAccNum);
}
