package com.cinnamon.dto.request;

import com.cinnamon.entity.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuizRequest {
    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    @NotBlank(message = "Tiêu đề quiz không được để trống")
    private String title;

    private QuizType type = QuizType.MULTIPLE_CHOICE;

    private Integer timeLimitSeconds = 300;

    private Integer xpReward = 20;

    @Valid
    private List<QuizQuestionRequest> questions;
}
