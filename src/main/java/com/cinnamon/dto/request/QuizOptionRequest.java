package com.cinnamon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuizOptionRequest {
    private Long id;

    @NotBlank(message = "Nội dung đáp án không được để trống")
    private String optionText;

    private Boolean isCorrect = false;
}
