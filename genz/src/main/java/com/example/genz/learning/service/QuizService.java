package com.example.genz.learning.service;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.dto.QuizResultResponse;
import com.example.genz.learning.dto.QuizStartResponse;
import com.example.genz.learning.dto.QuizSubmitRequest;
import com.example.genz.learning.dto.QuizUserEnergyResponse;

import java.util.UUID;

public interface QuizService {

    QuizStartResponse startQuiz(User user);

    QuizResultResponse submitQuiz(UUID quizAttemptId, QuizSubmitRequest request, User user);

    QuizResultResponse getQuizAttemptResult(UUID quizAttemptId, User user);

    QuizUserEnergyResponse getUserEnergyInfo(User user);
}
