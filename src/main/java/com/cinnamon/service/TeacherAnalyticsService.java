package com.cinnamon.service;

import com.cinnamon.dto.response.TeacherAnalyticsResponse;
import com.cinnamon.entity.Course;
import com.cinnamon.entity.Lesson;
import com.cinnamon.enums.Role;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TeacherAnalyticsService - Dịch vụ tổng hợp số liệu báo cáo và thống kê dành cho Giảng viên.
 * Cung cấp:
 * - Tổng số lượng khóa học, bài học, bài kiểm tra của giáo viên.
 * - Tổng số lượng học viên duy nhất (Distinct Students) đang theo học.
 * - Điểm số bài kiểm tra trung bình của học viên (được làm tròn qua AnalyticsCalculator).
 * - Bảng chi tiết từng khóa học: cấp độ CEFR, số bài học, số lượt hoàn thành.
 */
@Service
@RequiredArgsConstructor
public class TeacherAnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final UserProgressRepository userProgressRepository;

    /**
     * Thu thập và tính toán toàn bộ chỉ số thống kê của giáo viên.
     * Nếu tài khoản có vai trò ADMIN, hàm sẽ thống kê trên quy mô toàn bộ hệ thống.
     *
     * @param teacherEmail email của giáo viên hoặc admin
     * @return TeacherAnalyticsResponse chứa các số liệu tổng quan và chi tiết từng khóa
     */
    public TeacherAnalyticsResponse getAnalytics(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

        // Nếu là ADMIN thì lấy tất cả khóa học, nếu là TEACHER thì chỉ lấy các khóa do mình tạo
        List<Course> courses = (teacher.getRole() == Role.ADMIN)
                ? courseRepository.findAll()
                : courseRepository.findByTeacherId(teacher.getId());

        int totalCourses = courses.size();
        int totalLessons = 0;
        int totalQuizzes = 0;
        List<Long> allLessonIds = new ArrayList<>();
        List<TeacherAnalyticsResponse.CourseStatDto> courseStats = new ArrayList<>();

        for (Course c : courses) {
            List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(c.getId());
            totalLessons += lessons.size();

            List<Long> cLessonIds = lessons.stream().map(Lesson::getId).collect(Collectors.toList());
            allLessonIds.addAll(cLessonIds);

            long completedCount = 0;
            if (!cLessonIds.isEmpty()) {
                completedCount = userProgressRepository.countCompletedByLessonIds(cLessonIds);
                for (Long lid : cLessonIds) {
                    totalQuizzes += quizRepository.findByLessonId(lid).size();
                }
            }

            courseStats.add(TeacherAnalyticsResponse.CourseStatDto.builder()
                    .courseId(c.getId())
                    .courseTitle(c.getTitle())
                    .level(c.getLevel().name())
                    .lessonsCount(lessons.size())
                    .completedCount(completedCount)
                    .build());
        }

        long totalStudents = 0;
        double avgScore = 0.0;
        if (!allLessonIds.isEmpty()) {
            totalStudents = userProgressRepository.countDistinctStudentsByLessonIds(allLessonIds);
            Double score = userProgressRepository.averageScoreByLessonIds(allLessonIds);
            // Áp dụng lớp tính toán độc lập AnalyticsCalculator
            avgScore = AnalyticsCalculator.roundScore(score);
        }

        return TeacherAnalyticsResponse.builder()
                .totalCourses(totalCourses)
                .totalLessons(totalLessons)
                .totalStudents(totalStudents)
                .totalQuizzes(totalQuizzes)
                .averageQuizScore(avgScore)
                .courseStats(courseStats)
                .build();
    }
}
