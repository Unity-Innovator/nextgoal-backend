package com.example.genz.challenge.evaluator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeEvaluationResult {
    private BigDecimal progress;
    private boolean completed;
}
