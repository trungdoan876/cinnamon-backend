package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QuizOptionDto {
    private Long id;
    private String optionText;
    // Don't expose isCorrect when sending question to students, or only expose if requested
    private Boolean isCorrect;
}
