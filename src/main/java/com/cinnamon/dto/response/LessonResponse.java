package com.cinnamon.dto.response;

import com.cinnamon.entity.LessonType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LessonResponse {
    private Long id;
    private Long courseId;
    private String title;
    private LessonType type;
    private String content;
    private String audioUrl;
    private String videoUrl;
    private Integer orderIndex;
    private Integer xpReward;
    private Boolean isCompleted;
    private LocalDateTime createdAt;
}
