package com.example.genz.Service.GoalService;

import com.example.genz.DTO.GoalDTO.CreateGoalRequest;
import com.example.genz.DTO.GoalDTO.GoalResponse;
import com.example.genz.DTO.GoalDTO.UpdateGoalRequest;
import com.example.genz.Entity.Auth.User;
import com.example.genz.Entity.Goal.Goal;
import com.example.genz.Entity.Wallet.GrouplWallet;
import com.example.genz.Enums.Currency;
import com.example.genz.Enums.GoalStatus;
import com.example.genz.Repository.AuthRepository.UserRepository;
import com.example.genz.Repository.GoalRepository.GoalRepository;
import com.example.genz.Repository.QrCodeRepository.GoalWalletRepository;
import com.example.genz.exception.ResourceNoteFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;
    private final GoalWalletRepository goalWalletRepository;

    @Override
    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);

        Goal goal = new Goal();
        goal.setGoalName(request.getGoalName());
        goal.setTargetAmount(request.getTargetAmount());
        goal.setCurrentAmount(BigDecimal.ZERO);
        goal.setTargetDate(request.getTargetDate());
        goal.setCategory(request.getCategory());
        goal.setImageUrl(request.getImageUrl());
        goal.setStatus(GoalStatus.ACTIVE);
        goal.setOwner(owner);
        goal.setCreatedAt(LocalDateTime.now());

        Goal saved = goalRepository.save(goal);

        GrouplWallet wallet = new GrouplWallet();
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setUser(owner);
        wallet.setGoal(saved);
        wallet.setCurrency(request.getCurrency());
        goalWalletRepository.save(wallet);

        return toGoalResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoalResponse> getMyGoals(String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        return goalRepository.findByOwnerOrderByCreatedAtDesc(owner)
                .stream()
                .map(this::toGoalResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public GoalResponse getGoal(UUID goalId, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findByIdAndOwner(goalId, owner)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));
        return toGoalResponse(goal);
    }

    @Override
    @Transactional
    public GoalResponse updateGoal(UUID goalId, UpdateGoalRequest request, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        if (!goal.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have permission to update this goal");
        }

        if (request.getGoalName() != null) {
            goal.setGoalName(request.getGoalName());
        }
        if (request.getTargetAmount() != null) {
            goal.setTargetAmount(request.getTargetAmount());
        }
        if (request.getTargetDate() != null) {
            goal.setTargetDate(request.getTargetDate());
        }
        if (request.getCategory() != null) {
            goal.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            goal.setImageUrl(request.getImageUrl());
        }

        Goal saved = goalRepository.save(goal);
        return toGoalResponse(saved);
    }

    @Override
    @Transactional
    public void deleteGoal(UUID goalId, String phoneNumber) {
        User owner = getUserByPhone(phoneNumber);
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNoteFoundException("Goal not found"));

        if (!goal.getOwner().getId().equals(owner.getId())) {
            throw new AccessDeniedException("You do not have permission to delete this goal");
        }

        goal.setStatus(GoalStatus.CANCELLED);
        goalRepository.save(goal);
    }

    private User getUserByPhone(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ResourceNoteFoundException("User not found"));
    }

    private GoalResponse toGoalResponse(Goal goal) {
        return GoalResponse.builder()
                .goalId(goal.getId())
                .goalName(goal.getGoalName())
                .targetAmount(goal.getTargetAmount())
                .currentAmount(goal.getCurrentAmount())
                .targetDate(goal.getTargetDate())
                .category(goal.getCategory())
                .imageUrl(goal.getImageUrl())
                .status(goal.getStatus())
                .createdAt(goal.getCreatedAt())
                .completedAt(goal.getCompletedAt())
                .build();
    }
}
