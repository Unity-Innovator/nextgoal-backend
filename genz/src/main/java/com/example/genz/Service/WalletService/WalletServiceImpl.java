package com.example.genz.Service.WalletService;

import com.example.genz.DTO.WalletDTO.DepositRequest;
import com.example.genz.DTO.WalletDTO.DepositResponse;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Transaction.WalletTransaction;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Enums.TransactionStatus;
import com.example.genz.Enums.TransactionType;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.Repository.TransactionRepository.WalletTransactionRepository;
import com.example.genz.exception.ResourceNoteFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository userRepository;
    private final GoalWalletRepository goalWalletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    @Override
    @Transactional
    public DepositResponse deposit(DepositRequest request, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));

        GrouplWallet wallet = goalWalletRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNoteFoundException("Goal wallet not found for user"));

        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());
        wallet.setBalance(newBalance);
        goalWalletRepository.save(wallet);

        WalletTransaction transaction = new WalletTransaction();
        transaction.setWallet(wallet);
        transaction.setAmount(request.getAmount());
        //Not done
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Fake deposit (Development)");
        walletTransactionRepository.save(transaction);

        return new DepositResponse(true, "Deposit successful.", newBalance);
    }
}
