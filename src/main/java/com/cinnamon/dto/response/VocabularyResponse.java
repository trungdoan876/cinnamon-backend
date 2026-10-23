package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VocabularyResponse {
    private Long id;
    private Long lessonId;
    private String word;
    private String pronunciation;
    private String definition;
    private String exampleSentence;
    private String imageUrl;
    private String audioUrl;
}
