package com.example.genz.DTO.TransferDTO;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequestDTO {
    private String senderAccNum;
    private String receiverAccNum;
    private BigDecimal amount;
}
