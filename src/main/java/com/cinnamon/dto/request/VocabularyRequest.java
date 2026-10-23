package com.cinnamon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VocabularyRequest {
    @NotNull(message = "ID bài học không được để trống")
    private Long lessonId;

    @NotBlank(message = "Từ vựng không được để trống")
    private String word;

    private String pronunciation;
    private String definition;
    private String exampleSentence;
    private String imageUrl;
    private String audioUrl;
}
