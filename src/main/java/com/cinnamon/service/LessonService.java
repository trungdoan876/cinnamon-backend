package com.cinnamon.service;

import com.cinnamon.dto.request.LessonRequest;
import com.cinnamon.dto.response.LessonResponse;
import com.cinnamon.entity.*;
import com.cinnamon.enums.LessonType;
import com.cinnamon.enums.Role;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * LessonService - Dịch vụ quản lý các bài học đa phương tiện trong từng khóa học.
 * Đảm nhiệm:
 * - Lấy danh sách bài học và tự động gắn cờ hoàn thành (isCompleted) theo học viên đang đăng nhập.
 * - Xem chi tiết bài học (nội dung, liên kết audio, video).
 * - Tạo, sửa, xóa bài học (kiểm tra quyền sở hữu khóa học của giáo viên).
 * - Ghi nhận học viên hoàn thành bài học và tự động tích lũy điểm kinh nghiệm (XP).
 * - Xóa bài học dạng Cascade (xóa sạch tiến trình học, từ vựng, câu hỏi quiz liên quan).
 */
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
    private final GamificationService gamificationService;

    /**
     * Lấy danh sách các bài học thuộc một khóa học cụ thể, sắp xếp theo thứ tự orderIndex tăng dần.
     * Tự động kiểm tra tiến trình học của userEmail để đánh dấu bài học nào đã xong.
     *
     * @param courseId  ID khóa học
     * @param userEmail email của học viên đang đăng nhập (hoặc null nếu chưa đăng nhập)
     * @return Danh sách LessonResponse
     */
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

    /**
     * Xem nội dung chi tiết của một bài học theo ID.
     *
     * @param id        ID bài học
     * @param userEmail email của người dùng đang đăng nhập
     * @return LessonResponse chứa nội dung bài học và trạng thái hoàn thành
     */
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

    /**
     * Tạo một bài học mới thuộc một khóa học.
     *
     * @param request chứa courseId, title, type (VOCABULARY, GRAMMAR, LISTENING...), content, audioUrl, videoUrl, xpReward
     * @return LessonResponse vừa tạo
     */
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

    /**
     * Cập nhật thông tin bài học. Yêu cầu người thao tác phải sở hữu khóa học chứa bài học này.
     *
     * @param id        ID bài học cần sửa
     * @param request   dữ liệu mới
     * @param userEmail email của người đang thao tác
     * @return LessonResponse sau khi lưu
     */
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

    /**
     * Đánh dấu học viên đã hoàn thành một bài học.
     * - Nếu bài học chưa từng hoàn thành trước đó: Đánh dấu isCompleted = true, ghi nhận ngày giờ.
     * - Tự động cộng điểm XP thưởng của bài học vào ví UserXp của học viên.
     * - Nếu bài học đã hoàn thành trước đó: Không cộng thêm XP để chống cày điểm lặp lại.
     *
     * @param lessonId  ID bài học vừa học xong
     * @param userEmail email của học viên
     * @return số điểm XP học viên được cộng (0 nếu đã hoàn thành từ trước)
     */
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

            gamificationService.addXp(userXp, xpAwarded);
            userXpRepository.save(userXp);
        }

        return xpAwarded;
    }

    /**
     * Xóa một bài học theo ID.
     *
     * @param id        ID bài học cần xóa
     * @param userEmail email người thao tác
     */
    @Transactional
    public void deleteLesson(Long id, String userEmail) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bài học"));

        validateCourseOwnership(lesson.getCourse(), userEmail);

        deleteLessonCascade(lesson);
    }

    /**
     * Kiểm tra giáo viên có phải là chủ sở hữu của khóa học hay không.
     */
    private void validateCourseOwnership(Course course, String userEmail) {
        if (userEmail == null || course == null) return;
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null || user.getRole() == Role.ADMIN) return;

        if (course.getTeacher() != null && !course.getTeacher().getId().equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên bài học của khóa học khác");
        }
    }

    /**
     * Xóa toàn bộ bài học thuộc một khóa học (dùng khi xóa một khóa học).
     */
    @Transactional
    public void deleteLessonsByCourseId(Long courseId) {
        for (Lesson lesson : lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId)) {
            deleteLessonCascade(lesson);
        }
    }

    /**
     * Xoá bài học dạng Cascade: Xóa sạch tiến trình học, từ vựng và toàn bộ câu hỏi/lựa chọn quiz liên quan.
     */
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

    /**
     * Chuyển đổi đối tượng Lesson Entity sang DTO LessonResponse.
     */
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
                .build();
    }
}
