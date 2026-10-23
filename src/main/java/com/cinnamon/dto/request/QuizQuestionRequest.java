package com.cinnamon.dto.request;

import com.cinnamon.entity.QuizType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class QuizQuestionRequest {
    private Long id;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    private String questionText;

    private QuizType questionType = QuizType.MULTIPLE_CHOICE;

    private Integer orderIndex = 0;

    @NotEmpty(message = "Câu hỏi cần có ít nhất 2 đáp án")
    @Valid
    private List<QuizOptionRequest> options;
}
