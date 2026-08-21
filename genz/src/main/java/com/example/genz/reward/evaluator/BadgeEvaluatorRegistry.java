package com.example.genz.reward.evaluator;

import com.example.genz.reward.enums.BadgeRequirementType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BadgeEvaluatorRegistry {

    private final List<BadgeRequirementEvaluator> evaluators;

    public Optional<BadgeRequirementEvaluator> getEvaluator(BadgeRequirementType requirementType) {
        return evaluators.stream()
                .filter(evaluator -> evaluator.supports(requirementType))
                .findFirst();
    }
}
