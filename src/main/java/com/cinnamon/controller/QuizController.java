package com.cinnamon.controller;

import com.cinnamon.dto.request.QuizSubmitRequest;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.QuizResponse;
import com.cinnamon.dto.response.QuizResultResponse;
import com.cinnamon.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping("/lessons/{lessonId}/quizzes")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByLesson(@PathVariable Long lessonId) {
        List<QuizResponse> quizzes = quizService.getQuizzesByLessonId(lessonId);
        return ResponseEntity.ok(ApiResponse.success(quizzes));
    }

    @GetMapping("/quizzes/{id}")
    public ResponseEntity<ApiResponse<QuizResponse>> getQuizById(@PathVariable Long id) {
        QuizResponse quiz = quizService.getQuizById(id, false);
        return ResponseEntity.ok(ApiResponse.success(quiz));
    }

    @GetMapping("/teacher/lessons/{lessonId}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<QuizResponse>>> getQuizzesByLessonForTeacher(@PathVariable Long lessonId) {
        List<QuizResponse> quizzes = quizService.getQuizzesByLessonIdForTeacher(lessonId);
        return ResponseEntity.ok(ApiResponse.success(quizzes));
    }

    @PostMapping("/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> createQuiz(@Valid @RequestBody com.cinnamon.dto.request.QuizRequest request) {
        QuizResponse quiz = quizService.createQuiz(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo quiz thành công", quiz));
    }

    @PutMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<QuizResponse>> updateQuiz(
            @PathVariable Long id,
            @Valid @RequestBody com.cinnamon.dto.request.QuizRequest request) {
        QuizResponse quiz = quizService.updateQuiz(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quiz thành công", quiz));
    }

    @DeleteMapping("/quizzes/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok(ApiResponse.message("Xóa quiz thành công"));
    }

    @PostMapping("/quizzes/{id}/submit")
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuiz(
            @PathVariable Long id,
            @Valid @RequestBody QuizSubmitRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        request.setQuizId(id);
        QuizResultResponse result = quizService.submitQuiz(request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(result, "Nộp bài quiz thành công"));
    }
}
