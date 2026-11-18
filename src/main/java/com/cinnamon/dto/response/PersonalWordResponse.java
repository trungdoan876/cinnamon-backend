package com.cinnamon.dto.response;

import com.cinnamon.entity.PersonalWord;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PersonalWordResponse {
    private Long id;
    private Long listId;
    private String word;
    private String pronunciation;
    private String definition;
    private String exampleSentence;
    private Boolean mastered;
    private Integer reviewCount;

    public static PersonalWordResponse from(PersonalWord w) {
        return PersonalWordResponse.builder()
                .id(w.getId())
                .listId(w.getList() == null ? null : w.getList().getId())
                .word(w.getWord())
                .pronunciation(w.getPronunciation())
                .definition(w.getDefinition())
                .exampleSentence(w.getExampleSentence())
                .mastered(w.getMastered())
                .reviewCount(w.getReviewCount())
                .build();
    }
}

