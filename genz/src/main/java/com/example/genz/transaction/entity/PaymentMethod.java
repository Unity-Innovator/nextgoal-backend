package com.example.genz.transaction.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.transaction.enums.Currency;
import com.example.genz.transaction.enums.PaymentMethodStatus;
import com.example.genz.transaction.enums.PaymentMethodType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "payment_method_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "method_type", nullable = false)
    private PaymentMethodType methodType;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String provider;

    @NotBlank
    @Size(max = 200)
    @Column(name = "account_name", nullable = false)
    private String accountName;

    @NotBlank
    @Size(max = 50)
    @Column(name = "account_number_masked", nullable = false)
    private String accountNumberMasked;

    @Size(max = 500)
    @Column(name = "account_token")
    private String accountToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    @Column(name = "is_default")
    private Boolean isDefault;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodStatus status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
