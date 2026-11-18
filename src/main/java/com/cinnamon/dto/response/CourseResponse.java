package com.cinnamon.dto.response;

import com.cinnamon.enums.CourseLevel;
import lombok.Builder;
import lombok.Data;

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
    private int totalLessons;
}
