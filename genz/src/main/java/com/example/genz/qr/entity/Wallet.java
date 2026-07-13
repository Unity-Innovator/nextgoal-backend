package com.example.genz.qr.entity;
import com.example.genz.auth.entity.User;
import com.example.genz.qr.enums.Currency;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "wallets")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "wallet_id", columnDefinition = "uuid",updatable = false, nullable = false)
    private UUID id;

    @ManyToOne()
    @JoinColumn(name = "user_id")
    private User users;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false,unique = true,name = "account_number")
    private String accountNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;
}
