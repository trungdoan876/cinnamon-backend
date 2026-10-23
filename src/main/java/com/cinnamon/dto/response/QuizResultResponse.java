package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class QuizResultResponse {
    private Long quizId;
    private int totalQuestions;
    private int correctCount;
    private int score; // e.g. 0-100%
    private int xpEarned;
    private boolean passed;
    private Map<Long, Long> correctAnswers; // questionId -> correctOptionId
}
