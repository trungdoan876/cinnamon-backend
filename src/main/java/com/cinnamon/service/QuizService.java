package com.cinnamon.service;

import com.cinnamon.dto.request.QuizOptionRequest;
import com.cinnamon.dto.request.QuizQuestionRequest;
import com.cinnamon.dto.request.QuizRequest;
import com.cinnamon.dto.request.QuizSubmitRequest;
import com.cinnamon.dto.response.QuizOptionDto;
import com.cinnamon.dto.response.QuizQuestionDto;
import com.cinnamon.dto.response.QuizResponse;
import com.cinnamon.dto.response.QuizResultResponse;
import com.cinnamon.entity.*;
import com.cinnamon.enums.QuizType;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * QuizService - Dịch vụ quản lý đề thi, câu hỏi trắc nghiệm và quy trình nộp bài chấm điểm.
 * Đảm nhiệm:
 * - Tạo, cập nhật và xóa bài Quiz kèm danh sách câu hỏi và lựa chọn (Option A, B, C, D).
 * - Lấy đề thi cho học viên (ẩn đáp án đúng để chống gian lận) hoặc cho giáo viên (hiện đầy đủ).
 * - Thu thập câu trả lời của học sinh, ủy quyền cho QuizEvaluator chấm điểm độc lập.
 * - Ghi nhận điểm số vào UserProgress và cộng điểm XP thưởng vào UserXp.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizOptionRepository quizOptionRepository;
    private final UserProgressRepository userProgressRepository;
    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final LessonRepository lessonRepository;
    private final GamificationService gamificationService;

    /**
     * Lấy danh sách các bài Quiz trong một bài học dành cho Học viên.
     * Tự động ẩn đáp án đúng (isCorrect = null) để chống xem trước đáp án.
     *
     * @param lessonId ID bài học
     * @return Danh sách QuizResponse đã che đáp án đúng
     */
    public List<QuizResponse> getQuizzesByLessonId(Long lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .stream()
                .map(quiz -> getQuizById(quiz.getId(), false))
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách các bài Quiz trong bài học dành cho Giáo viên / Quản trị viên.
     * Hiển thị đầy đủ cờ đáp án đúng (isCorrect = true/false) để giáo viên kiểm tra đề thi.
     *
     * @param lessonId ID bài học
     * @return Danh sách QuizResponse đầy đủ đáp án
     */
    public List<QuizResponse> getQuizzesByLessonIdForTeacher(Long lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .stream()
                .map(quiz -> getQuizById(quiz.getId(), true))
                .collect(Collectors.toList());
    }

    /**
     * Tạo mới một bài Quiz kèm toàn bộ câu hỏi và lựa chọn trắc nghiệm.
     *
     * @param request chứa lessonId, title, type, timeLimitSeconds, xpReward, và danh sách câu hỏi
     * @return QuizResponse chi tiết của bài thi vừa tạo
     */
    @Transactional
    public QuizResponse createQuiz(QuizRequest request) {
        Lesson lesson = lessonRepository.findById(request.getLessonId())
                .orElseThrow(() -> new ResourceNotFoundException("Bài học không tồn tại"));

        Quiz quiz = Quiz.builder()
                .lesson(lesson)
                .title(request.getTitle())
                .type(request.getType() != null ? request.getType() : QuizType.MULTIPLE_CHOICE)
                .timeLimitSeconds(request.getTimeLimitSeconds() != null ? request.getTimeLimitSeconds() : 180)
                .xpReward(request.getXpReward() != null ? request.getXpReward() : 20)
                .build();
        quiz = quizRepository.save(quiz);

        saveQuestionsAndOptions(quiz, request.getQuestions());

        return getQuizById(quiz.getId(), true);
    }

    /**
     * Cập nhật thông tin bài Quiz và thay mới danh sách câu hỏi / lựa chọn.
     *
     * @param id      ID bài quiz cần sửa
     * @param request dữ liệu cập nhật
     * @return QuizResponse sau khi cập nhật
     */
    @Transactional
    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        quiz.setTitle(request.getTitle());
        if (request.getType() != null) quiz.setType(request.getType());
        if (request.getTimeLimitSeconds() != null) quiz.setTimeLimitSeconds(request.getTimeLimitSeconds());
        if (request.getXpReward() != null) quiz.setXpReward(request.getXpReward());
        quizRepository.save(quiz);

        // Xóa câu hỏi và lựa chọn cũ rồi tạo lại theo danh sách mới
        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(id);
        for (QuizQuestion q : existingQuestions) {
            quizOptionRepository.deleteAll(quizOptionRepository.findByQuestionId(q.getId()));
        }
        quizQuestionRepository.deleteAll(existingQuestions);

        saveQuestionsAndOptions(quiz, request.getQuestions());

        return getQuizById(quiz.getId(), true);
    }

    /**
     * Xóa bài kiểm tra và toàn bộ câu hỏi/lựa chọn liên quan.
     *
     * @param id ID bài quiz cần xóa
     */
    @Transactional
    public void deleteQuiz(Long id) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(id);
        for (QuizQuestion q : questions) {
            quizOptionRepository.deleteAll(quizOptionRepository.findByQuestionId(q.getId()));
        }
        quizQuestionRepository.deleteAll(questions);
        quizRepository.delete(quiz);
    }

    /**
     * Lưu danh sách câu hỏi và các lựa chọn đáp án tương ứng vào Database.
     */
    private void saveQuestionsAndOptions(Quiz quiz, List<QuizQuestionRequest> questionRequests) {
        if (questionRequests == null) return;

        int qIndex = 1;
        for (QuizQuestionRequest qReq : questionRequests) {
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(qReq.getQuestionText())
                    .questionType(qReq.getQuestionType() != null ? qReq.getQuestionType() : QuizType.MULTIPLE_CHOICE)
                    .orderIndex(qReq.getOrderIndex() != null ? qReq.getOrderIndex() : qIndex++)
                    .build();
            question = quizQuestionRepository.save(question);

            if (qReq.getOptions() != null) {
                for (QuizOptionRequest optReq : qReq.getOptions()) {
                    QuizOption option = QuizOption.builder()
                            .question(question)
                            .optionText(optReq.getOptionText())
                            .isCorrect(Boolean.TRUE.equals(optReq.getIsCorrect()))
                            .build();
                    quizOptionRepository.save(option);
                }
            }
        }
    }

    /**
     * Lấy bài kiểm tra theo ID, kèm tùy chọn có hiển thị đáp án đúng hay không.
     *
     * @param quizId        ID bài quiz
     * @param revealAnswers true = hiện đáp án (cho giáo viên), false = ẩn đáp án (cho học sinh)
     * @return QuizResponse
     */
    public QuizResponse getQuizById(Long quizId, boolean revealAnswers) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quizId);

        List<QuizQuestionDto> questionDtos = questions.stream().map(q -> {
            List<QuizOption> options = quizOptionRepository.findByQuestionId(q.getId());
            List<QuizOptionDto> optionDtos = options.stream().map(opt -> QuizOptionDto.builder()
                    .id(opt.getId())
                    .optionText(opt.getOptionText())
                    .isCorrect(revealAnswers ? opt.getIsCorrect() : null)
                    .build()).collect(Collectors.toList());

            return QuizQuestionDto.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType())
                    .orderIndex(q.getOrderIndex())
                    .options(optionDtos)
                    .build();
        }).collect(Collectors.toList());

        return QuizResponse.builder()
                .id(quiz.getId())
                .lessonId(quiz.getLesson().getId())
                .title(quiz.getTitle())
                .type(quiz.getType())
                .timeLimitSeconds(quiz.getTimeLimitSeconds())
                .xpReward(quiz.getXpReward())
                .questions(questionDtos)
                .build();
    }

    /**
     * Nộp bài kiểm tra và chấm điểm tự động.
     * Quy trình:
     * 1. Thu thập đáp án đúng của từng câu hỏi từ DB.
     * 2. Gọi QuizEvaluator.evaluate(...) để chấm điểm và tính toán XP.
     * 3. Ghi nhận kết quả vào UserProgress qua method progress.recordResult(...).
     * 4. Tự động cộng XP thưởng vào UserXp qua method userXp.addXp(...).
     *
     * @param request   chứa quizId và bản đồ câu trả lời của học sinh
     * @param userEmail email học sinh nộp bài
     * @return QuizResultResponse chứa số câu đúng, %, đỗ/trượt, XP đạt được và danh sách đáp án đúng
     */
    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmitRequest request, String userEmail) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        Map<Long, Long> correctAnswers = new HashMap<>();

        for (QuizQuestion q : questions) {
            List<QuizOption> options = quizOptionRepository.findByQuestionId(q.getId());
            options.stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .findFirst()
                    .ifPresent(opt -> correctAnswers.put(q.getId(), opt.getId()));
        }

        // Chấm điểm bằng QuizEvaluator (Pure Domain Evaluator - 100% Testable)
        QuizEvaluator.Result evalResult = QuizEvaluator.evaluate(
                questions.size(),
                correctAnswers,
                request.getAnswers(),
                quiz.getXpReward()
        );

        // Lưu tiến trình cho bài học sử dụng OOP Encapsulation (Tell, Don't Ask)
        Lesson lesson = quiz.getLesson();
        UserProgress progress = userProgressRepository.findByUserIdAndLessonId(user.getId(), lesson.getId())
                .orElse(UserProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        progress.setScore(evalResult.getScorePercent());
        if (evalResult.isPassed()) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }
        userProgressRepository.save(progress);

        // Cộng XP bằng hàm nghiệp vụ trong GamificationService
        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElseGet(() -> UserXp.builder().user(user).totalXp(0).level(1).weeklyXp(0).build());
        gamificationService.addXp(userXp, evalResult.getXpEarned());
        userXpRepository.save(userXp);

        return QuizResultResponse.builder()
                .quizId(quiz.getId())
                .totalQuestions(evalResult.getTotalQuestions())
                .correctCount(evalResult.getCorrectCount())
                .score(evalResult.getScorePercent())
                .xpEarned(evalResult.getXpEarned())
                .passed(evalResult.isPassed())
                .correctAnswers(correctAnswers)
                .build();
    }
}
