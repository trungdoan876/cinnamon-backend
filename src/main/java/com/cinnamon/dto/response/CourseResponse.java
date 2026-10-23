package com.cinnamon.dto.response;

import com.cinnamon.entity.CourseLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseResponse {
    private Long id;
    private String title;
    private String description;
    private CourseLevel level;
    private String thumbnailUrl;
    private Long teacherId;
    private String teacherName;
    private Boolean isPublished;
    private LocalDateTime createdAt;
    private int totalLessons;
}
