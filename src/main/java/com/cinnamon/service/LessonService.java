package com.cinnamon.service;

import com.cinnamon.dto.request.LessonRequest;
import com.cinnamon.dto.response.LessonResponse;
import com.cinnamon.entity.Course;
import com.cinnamon.entity.Lesson;
import com.cinnamon.entity.Quiz;
import com.cinnamon.entity.QuizQuestion;
import com.cinnamon.entity.User;
import com.cinnamon.entity.UserProgress;
import com.cinnamon.entity.UserXp;
import com.cinnamon.entity.Vocabulary;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.CourseRepository;
import com.cinnamon.repository.LessonRepository;
import com.cinnamon.repository.QuizOptionRepository;
import com.cinnamon.repository.QuizQuestionRepository;
import com.cinnamon.repository.QuizRepository;
import com.cinnamon.repository.UserProgressRepository;
import com.cinnamon.repository.UserRepository;
import com.cinnamon.repository.UserXpRepository;
import com.cinnamon.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final VocabularyRepository vocabularyRepository;
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;

    public List<LessonResponse> getLessonsByCourseId(Long courseId, String userEmail) {
        Long userId = null;
        if (userEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                userId = userOpt.get().getId();
            }
        }

        final Long currentUserId = userId;
        List<Lesson> lessons = lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        return lessons.stream().map(lesson -> {
            boolean isCompleted = false;
            if (currentUserId != null) {
                isCompleted = userProgressRepository
                        .findByUserIdAndLessonId(currentUserId, lesson.getId())
                        .map(UserProgress::getIsCompleted)
                        .orElse(false);
            }
            return toResponse(lesson, isCompleted);
        }).collect(Collectors.toList());
    }

    public LessonResponse getLessonById(Long id, String userEmail) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học với id: " + id));

        boolean isCompleted = false;
        if (userEmail != null) {
            Optional<User> userOpt = userRepository.findByEmail(userEmail);
            if (userOpt.isPresent()) {
                isCompleted = userProgressRepository
                        .findByUserIdAndLessonId(userOpt.get().getId(), lesson.getId())
                        .map(UserProgress::getIsCompleted)
                        .orElse(false);
            }
        }

        return toResponse(lesson, isCompleted);
    }

    @Transactional
    public LessonResponse createLesson(LessonRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy khóa học"));

        Lesson lesson = Lesson.builder()
                .course(course)
                .title(request.getTitle())
                .type(request.getType())
                .content(request.getContent())
                .audioUrl(request.getAudioUrl())
                .videoUrl(request.getVideoUrl())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .xpReward(request.getXpReward() != null ? request.getXpReward() : 10)
                .build();

        return toResponse(lessonRepository.save(lesson), false);
    }

    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest request, String userEmail) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));

        validateCourseOwnership(lesson.getCourse(), userEmail);

        lesson.setTitle(request.getTitle());
        lesson.setType(request.getType());
        lesson.setContent(request.getContent());
        lesson.setAudioUrl(request.getAudioUrl());
        lesson.setVideoUrl(request.getVideoUrl());
        if (request.getOrderIndex() != null) {
            lesson.setOrderIndex(request.getOrderIndex());
        }
        if (request.getXpReward() != null) {
            lesson.setXpReward(request.getXpReward());
        }

        return toResponse(lessonRepository.save(lesson), false);
    }

    @Transactional
    public int completeLesson(Long lessonId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Bài học không tồn tại"));

        Optional<UserProgress> progressOpt = userProgressRepository.findByUserIdAndLessonId(user.getId(), lessonId);
        boolean alreadyCompleted = progressOpt.map(UserProgress::getIsCompleted).orElse(false);

        int xpAwarded = 0;
        if (!alreadyCompleted) {
            UserProgress progress = progressOpt.orElse(UserProgress.builder()
                    .user(user)
                    .lesson(lesson)
                    .build());

            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            userProgressRepository.save(progress);

            xpAwarded = (lesson.getXpReward() != null && lesson.getXpReward() > 0) ? lesson.getXpReward() : 15;

            // Cộng XP cho UserXp
            UserXp userXp = userXpRepository.findByUserId(user.getId())
                    .orElseGet(() -> UserXp.builder().user(user).totalXp(0).level(1).weeklyXp(0).build());

            userXp.addXp(xpAwarded);
            userXpRepository.save(userXp);
        }

        return xpAwarded;
    }

    @Transactional
    public void deleteLesson(Long id, String userEmail) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));

        validateCourseOwnership(lesson.getCourse(), userEmail);

        deleteLessonCascade(lesson);
    }

    private void validateCourseOwnership(Course course, String userEmail) {
        if (userEmail == null || course == null) return;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || user.getRole() == com.cinnamon.entity.Role.ADMIN) return;

        if (course.getTeacher() != null && !course.getTeacher().getId().equals(user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thao tác trên bài học của khóa học khác");
        }
    }

    @Transactional
    public void deleteLessonsByCourseId(Long courseId) {
        for (Lesson lesson : lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)) {
            deleteLessonCascade(lesson);
        }
    }

    // Xoá bài học kèm dữ liệu con (tiến độ học, từ vựng, quiz) để không vi phạm khoá ngoại
    private void deleteLessonCascade(Lesson lesson) {
        userProgressRepository.deleteAll(userProgressRepository.findByLessonId(lesson.getId()));
        vocabularyRepository.deleteAll(vocabularyRepository.findByLessonId(lesson.getId()));

        List<Quiz> quizzes = quizRepository.findByLessonId(lesson.getId());
        for (Quiz quiz : quizzes) {
            List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
            for (QuizQuestion question : questions) {
                quizOptionRepository.deleteAll(quizOptionRepository.findByQuestionId(question.getId()));
            }
            quizQuestionRepository.deleteAll(questions);
        }
        quizRepository.deleteAll(quizzes);

        lessonRepository.delete(lesson);
    }

    private LessonResponse toResponse(Lesson lesson, Boolean isCompleted) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .courseId(lesson.getCourse().getId())
                .title(lesson.getTitle())
                .type(lesson.getType())
                .content(lesson.getContent())
                .audioUrl(lesson.getAudioUrl())
                .videoUrl(lesson.getVideoUrl())
                .orderIndex(lesson.getOrderIndex())
                .xpReward(lesson.getXpReward())
                .isCompleted(isCompleted)
                .createdAt(lesson.getCreatedAt())
                .build();
    }
}
