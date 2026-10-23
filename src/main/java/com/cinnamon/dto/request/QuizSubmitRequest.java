package com.cinnamon.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class QuizSubmitRequest {
    @NotNull
    private Long quizId;

    // questionId -> selectedOptionId
    private Map<Long, Long> answers;
}
