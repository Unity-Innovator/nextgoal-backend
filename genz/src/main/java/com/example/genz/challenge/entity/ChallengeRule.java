package com.example.genz.challenge.entity;

import com.example.genz.challenge.enums.ChallengeRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChallengeRule {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type")
    private ChallengeRuleType ruleType;

    @Column(name = "minimum_amount", precision = 19, scale = 4)
    private BigDecimal minimumAmount;

    @Column(name = "target_count")
    private Integer targetCount;
}
