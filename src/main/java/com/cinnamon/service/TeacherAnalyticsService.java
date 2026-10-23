package com.cinnamon.service;

import com.cinnamon.dto.response.TeacherAnalyticsResponse;
import com.cinnamon.entity.Course;
import com.cinnamon.entity.Lesson;
import com.cinnamon.entity.Role;
import com.cinnamon.entity.User;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherAnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final QuizRepository quizRepository;
    private final UserProgressRepository userProgressRepository;

    public TeacherAnalyticsResponse getAnalytics(String teacherEmail) {
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Giáo viên không tồn tại"));

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
            if (score != null) {
                avgScore = Math.round(score * 10.0) / 10.0;
            }
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
