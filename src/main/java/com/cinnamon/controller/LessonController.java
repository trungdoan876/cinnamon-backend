package com.cinnamon.controller;

import com.cinnamon.dto.request.LessonRequest;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.LessonResponse;
import com.cinnamon.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/courses/{courseId}/lessons")
    public ResponseEntity<ApiResponse<List<LessonResponse>>> getLessonsByCourse(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        List<LessonResponse> lessons = lessonService.getLessonsByCourseId(courseId, email);
        return ResponseEntity.ok(ApiResponse.success(lessons));
    }

    @GetMapping("/lessons/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : null;
        LessonResponse lesson = lessonService.getLessonById(id, email);
        return ResponseEntity.ok(ApiResponse.success(lesson));
    }

    @PostMapping("/lessons")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(@Valid @RequestBody LessonRequest request) {
        LessonResponse created = lessonService.createLesson(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo bài học thành công"));
    }

    @PutMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(
            @PathVariable Long id,
            @Valid @RequestBody LessonRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        LessonResponse updated = lessonService.updateLesson(id, request, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật bài học thành công"));
    }

    @DeleteMapping("/lessons/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        lessonService.deleteLesson(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.message("Xóa bài học thành công"));
    }

    @PostMapping("/lessons/{id}/complete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> completeLesson(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        int xpEarned = lessonService.completeLesson(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("lessonId", id, "xpEarned", xpEarned, "message", "Chúc mừng bạn đã hoàn thành bài học!")
        ));
    }
}
