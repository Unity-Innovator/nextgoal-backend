package com.example.genz.Mapper;

import com.example.genz.DTO.TransferDTO.TransferResponseDTO;
import com.example.genz.Entity.Transaction.Transfer;

public class TrancationMapper {
    public static TransferResponseDTO toInfoResponse(Transfer transactions){
        TransferResponseDTO responseDTO=new TransferResponseDTO();
        responseDTO.setId(transactions.getId());
        responseDTO.setFullNameSender(transactions.getSenderAccount().getUsers().getFullName());
        responseDTO.setAccountNumberSender(transactions.getSenderAccount().getAccountNumber());
        responseDTO.setAmount(transactions.getAmount());
        responseDTO.setFullNameReceiver(transactions.getReceiverAccount().getUsers().getFullName());
        responseDTO.setDate(transactions.getCreatedAt().toLocalDate());

        return responseDTO;
    }
}
