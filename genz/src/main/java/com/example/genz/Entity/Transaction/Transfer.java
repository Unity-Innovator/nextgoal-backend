package com.example.genz.Entity.Transaction;

import com.example.genz.Entity.QrCode.Wallet;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "transactions")
public class Transfer {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "transaction_id", columnDefinition = "uuid",updatable = false, nullable = false)
    private UUID id;

    private BigDecimal amount;

    @ManyToOne
    @JoinColumn(name = "sender_account_id")
    private Wallet senderAccount;

    @ManyToOne
    @JoinColumn(name = "receiver_account_id")
    private Wallet receiverAccount;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
