package com.example.genz.DTO.TransferDTO;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class TransferResponseDTO {
        private UUID id;
        private String fullNameSender;
        private String accountNumberSender;
        private BigDecimal amount;
        private String fullNameReceiver;
        private LocalDate date;
}
