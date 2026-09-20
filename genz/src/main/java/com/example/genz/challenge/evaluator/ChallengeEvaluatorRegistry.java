package com.example.genz.challenge.evaluator;

import com.example.genz.challenge.enums.ChallengeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ChallengeEvaluatorRegistry {

    private final List<ChallengeEvaluator> evaluators;

    public Optional<ChallengeEvaluator> getEvaluator(ChallengeType type) {
        return evaluators.stream()
                .filter(evaluator -> evaluator.supports(type))
                .findFirst();
    }
}
