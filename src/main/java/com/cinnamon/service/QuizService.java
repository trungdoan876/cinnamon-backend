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
import com.cinnamon.exception.BadRequestException;
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

    public List<QuizResponse> getQuizzesByLessonId(Long lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .stream()
                .map(quiz -> getQuizById(quiz.getId(), false))
                .collect(Collectors.toList());
    }

    public List<QuizResponse> getQuizzesByLessonIdForTeacher(Long lessonId) {
        return quizRepository.findByLessonId(lessonId)
                .stream()
                .map(quiz -> getQuizById(quiz.getId(), true))
                .collect(Collectors.toList());
    }

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

    @Transactional
    public QuizResponse updateQuiz(Long id, QuizRequest request) {
        Quiz quiz = quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        quiz.setTitle(request.getTitle());
        if (request.getType() != null) quiz.setType(request.getType());
        if (request.getTimeLimitSeconds() != null) quiz.setTimeLimitSeconds(request.getTimeLimitSeconds());
        if (request.getXpReward() != null) quiz.setXpReward(request.getXpReward());
        quizRepository.save(quiz);

        // Delete existing questions and options, then re-create
        List<QuizQuestion> existingQuestions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(id);
        for (QuizQuestion q : existingQuestions) {
            quizOptionRepository.deleteAll(quizOptionRepository.findByQuestionId(q.getId()));
        }
        quizQuestionRepository.deleteAll(existingQuestions);

        saveQuestionsAndOptions(quiz, request.getQuestions());

        return getQuizById(quiz.getId(), true);
    }

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

    @Transactional
    public QuizResultResponse submitQuiz(QuizSubmitRequest request, String userEmail) {
        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new ResourceNotFoundException("Quiz không tồn tại"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByOrderIndexAsc(quiz.getId());
        int totalQuestions = questions.size();
        int correctCount = 0;
        Map<Long, Long> correctAnswers = new HashMap<>();

        Map<Long, Long> submittedAnswers = request.getAnswers() != null ? request.getAnswers() : new HashMap<>();

        for (QuizQuestion q : questions) {
            List<QuizOption> options = quizOptionRepository.findByQuestionId(q.getId());
            QuizOption correctOption = options.stream()
                    .filter(opt -> Boolean.TRUE.equals(opt.getIsCorrect()))
                    .findFirst()
                    .orElse(null);

            if (correctOption != null) {
                correctAnswers.put(q.getId(), correctOption.getId());
                Long userSelectedOptionId = submittedAnswers.get(q.getId());
                if (userSelectedOptionId != null && userSelectedOptionId.equals(correctOption.getId())) {
                    correctCount++;
                }
            }
        }

        int scorePercent = totalQuestions > 0 ? (int) Math.round(((double) correctCount / totalQuestions) * 100) : 100;
        boolean passed = scorePercent >= 60; // Pass nếu đạt >= 60%
        int xpEarned = passed ? (quiz.getXpReward() != null ? quiz.getXpReward() : 20) : 5;

        // Lưu tiến trình cho lesson
        Lesson lesson = quiz.getLesson();
        UserProgress progress = userProgressRepository.findByUserIdAndLessonId(user.getId(), lesson.getId())
                .orElse(UserProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .build());

        if (passed) {
            progress.setIsCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
        }
        progress.setScore(scorePercent);
        userProgressRepository.save(progress);

        // Cộng XP
        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElseGet(() -> UserXp.builder().user(user).totalXp(0).level(1).weeklyXp(0).build());
        userXp.addXp(xpEarned);
        userXpRepository.save(userXp);

        return QuizResultResponse.builder()
                .quizId(quiz.getId())
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .score(scorePercent)
                .xpEarned(xpEarned)
                .passed(passed)
                .correctAnswers(correctAnswers)
                .build();
    }
}
