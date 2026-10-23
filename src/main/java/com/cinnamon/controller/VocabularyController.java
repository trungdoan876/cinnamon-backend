package com.cinnamon.controller;

import com.cinnamon.dto.request.VocabularyRequest;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.VocabularyResponse;
import com.cinnamon.service.VocabularyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping("/lessons/{lessonId}/vocabulary")
    public ResponseEntity<ApiResponse<List<VocabularyResponse>>> getVocabularyByLesson(@PathVariable Long lessonId) {
        List<VocabularyResponse> list = vocabularyService.getVocabulariesByLessonId(lessonId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/vocabulary")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> addVocabulary(@Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.addVocabulary(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Thêm từ vựng thành công"));
    }

    @PutMapping("/vocabulary/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VocabularyResponse>> updateVocabulary(
            @PathVariable Long id,
            @Valid @RequestBody VocabularyRequest request) {
        VocabularyResponse response = vocabularyService.updateVocabulary(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật từ vựng thành công"));
    }

    @DeleteMapping("/vocabulary/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteVocabulary(@PathVariable Long id) {
        vocabularyService.deleteVocabulary(id);
        return ResponseEntity.ok(ApiResponse.message("Xóa từ vựng thành công"));
    }
}
