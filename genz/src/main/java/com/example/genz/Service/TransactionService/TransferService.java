package com.example.genz.Service.TransactionService;

import com.example.genz.DTO.TransferDTO.TransferRequestDTO;
import com.example.genz.DTO.TransferDTO.TransferResponseDTO;
import com.example.genz.Entity.Transaction.Transfer;
import com.example.genz.Mapper.TrancationMapper;
import com.example.genz.Repository.TransactionRepository.TransferRepository;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Entity.Wallet.Wallet;
import com.example.genz.Repository.QrCodeRepository.WalletRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
public class TransferService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WalletRepository walletRepository;
    @Autowired
    private TransferRepository transferRepository;
    @Transactional
    public TransferResponseDTO transfer(Authentication authentication, TransferRequestDTO requestDTO) {

        String phoneNumber = authentication.getName();

        User sender = userRepository.findByPhoneNumber(phoneNumber).orElseThrow();

        Wallet senderAccount =
                walletRepository.findByAccountNumber(requestDTO.getSenderAccNum());

        Wallet receiverAccount = walletRepository.findByAccountNumber(requestDTO.getReceiverAccNum());

        //validate transfer
        TransferValidation(senderAccount,receiverAccount,sender,requestDTO);

        senderAccount.setBalance(
                senderAccount.getBalance().subtract(requestDTO.getAmount()));

        receiverAccount.setBalance(
                receiverAccount.getBalance().add(requestDTO.getAmount()));

        Transfer transactions=new Transfer();
        transactions.setAmount(requestDTO.getAmount());
        transactions.setSenderAccount(senderAccount);
        transactions.setReceiverAccount(receiverAccount);

        Transfer saved = transferRepository.saveAndFlush(transactions);

        System.out.println(saved.getCreatedAt());

        return TrancationMapper.toInfoResponse(saved);
    }

    private void TransferValidation(Wallet senderAccount,Wallet receiverAccount,User sender,TransferRequestDTO requestDTO){
        if (senderAccount == null) {
            throw new RuntimeException("Sender account not found");
        }

        if (receiverAccount == null) {
            throw new RuntimeException("Receiver account not found");
        }

        if (!senderAccount.getUsers().getId().equals(sender.getId())) {
            throw new RuntimeException("You do not own this account");
        }

        if (requestDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be greater than zero");
        }

        if (senderAccount.getBalance().compareTo(requestDTO.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
    }
}
