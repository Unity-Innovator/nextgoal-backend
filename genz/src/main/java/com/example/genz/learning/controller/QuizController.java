package com.example.genz.learning.controller;

import com.example.genz.auth.entity.User;
import com.example.genz.auth.repository.UserRepository;
import com.example.genz.common.api.ApiResponse;
import com.example.genz.common.exception.ResourceNoteFoundException;
import com.example.genz.learning.dto.QuizResultResponse;
import com.example.genz.learning.dto.QuizStartResponse;
import com.example.genz.learning.dto.QuizSubmitRequest;
import com.example.genz.learning.dto.QuizUserEnergyResponse;
import com.example.genz.learning.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final UserRepository userRepository;

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<QuizStartResponse>> startQuiz(Authentication authentication) {
        User user = getUser(authentication);
        QuizStartResponse response = quizService.startQuiz(user);
        return ResponseEntity.ok(ApiResponse.success("Quiz started successfully", response));
    }

    @PostMapping("/{quizAttemptId}/submit")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuiz(
            @PathVariable UUID quizAttemptId,
            @Valid @RequestBody QuizSubmitRequest request,
            Authentication authentication) {
        User user = getUser(authentication);
        QuizResultResponse response = quizService.submitQuiz(quizAttemptId, request, user);
        return ResponseEntity.ok(ApiResponse.success("Quiz submitted successfully", response));
    }

    @GetMapping("/{quizAttemptId}")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getQuizAttemptResult(
            @PathVariable UUID quizAttemptId,
            Authentication authentication) {
        User user = getUser(authentication);
        QuizResultResponse response = quizService.getQuizAttemptResult(quizAttemptId, user);
        return ResponseEntity.ok(ApiResponse.success("Quiz result retrieved successfully", response));
    }

    @GetMapping("/energy")
    public ResponseEntity<ApiResponse<QuizUserEnergyResponse>> getUserEnergyInfo(Authentication authentication) {
        User user = getUser(authentication);
        QuizUserEnergyResponse response = quizService.getUserEnergyInfo(user);
        return ResponseEntity.ok(ApiResponse.success("User energy info retrieved successfully", response));
    }

    private User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResourceNoteFoundException("User not authenticated");
        }
        return userRepository.findByPhoneNumber(authentication.getName())
                .orElseThrow(() -> new ResourceNoteFoundException("User not found with phone number: " + authentication.getName()));
    }
}
