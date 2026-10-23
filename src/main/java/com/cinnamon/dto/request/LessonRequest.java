package com.cinnamon.dto.request;

import com.cinnamon.entity.LessonType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LessonRequest {

    @NotNull(message = "ID khóa học không được để trống")
    private Long courseId;

    @NotBlank(message = "Tên bài học không được để trống")
    private String title;

    @NotNull(message = "Loại bài học không được để trống")
    private LessonType type;

    private String content;
    private String audioUrl;
    private String videoUrl;
    private Integer orderIndex = 0;
    private Integer xpReward = 10;
}
