package com.cinnamon.dto.response;

import com.cinnamon.entity.QuizType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizResponse {
    private Long id;
    private Long lessonId;
    private String title;
    private QuizType type;
    private Integer timeLimitSeconds;
    private Integer xpReward;
    private List<QuizQuestionDto> questions;
}
