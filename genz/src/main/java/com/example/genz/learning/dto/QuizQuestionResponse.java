package com.example.genz.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionResponse {
    private UUID id;
    private UUID lessonId;
    private String question;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer orderIndex;
}
