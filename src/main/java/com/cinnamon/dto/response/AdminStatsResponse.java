package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalStudents;
    private long totalTeachers;
    private long totalAdmins;
    private long totalCourses;
    private long totalLessons;
    private long totalQuizzes;
    private long totalCompletedLessons;
}
