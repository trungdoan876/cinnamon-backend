package com.cinnamon.dto.response;

import com.cinnamon.enums.QuizType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizQuestionDto {
    private Long id;
    private String questionText;
    private QuizType questionType;
    private Integer orderIndex;
    private List<QuizOptionDto> options;
}
