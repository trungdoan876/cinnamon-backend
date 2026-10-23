package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TeacherAnalyticsResponse {
    private int totalCourses;
    private int totalLessons;
    private long totalStudents;
    private int totalQuizzes;
    private double averageQuizScore;
    private List<CourseStatDto> courseStats;

    @Data
    @Builder
    public static class CourseStatDto {
        private Long courseId;
        private String courseTitle;
        private String level;
        private int lessonsCount;
        private long completedCount;
    }
}
