package com.cinnamon.controller;

import com.cinnamon.dto.request.PersonalWordRequest;
import com.cinnamon.dto.request.WordListRequest;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.PersonalWordResponse;
import com.cinnamon.dto.response.WordListResponse;
import com.cinnamon.service.PersonalWordService;
import com.cinnamon.service.WordListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API flashcard từ vựng cá nhân: danh sách từ và các từ bên trong. Yêu cầu đăng nhập. */
@RestController
@RequestMapping("/api/flashcards")
@RequiredArgsConstructor
public class PersonalWordController {

    private final PersonalWordService wordService;
    private final WordListService listService;

    // ===== Danh sách từ =====

    @GetMapping("/lists")
    public ResponseEntity<ApiResponse<List<WordListResponse>>> getLists(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ApiResponse.success(listService.list(user.getUsername())));
    }

    @PostMapping("/lists")
    public ResponseEntity<ApiResponse<WordListResponse>> createList(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody WordListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(listService.create(user.getUsername(), request), "Đã tạo danh sách"));
    }

    @PutMapping("/lists/{id}")
    public ResponseEntity<ApiResponse<WordListResponse>> updateList(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody WordListRequest request) {
        return ResponseEntity.ok(ApiResponse.success(listService.update(user.getUsername(), id, request), "Đã cập nhật danh sách"));
    }

    @DeleteMapping("/lists/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteList(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        listService.delete(user.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.message("Đã xóa danh sách"));
    }

    // ===== Từ trong danh sách =====

    @GetMapping
    public ResponseEntity<ApiResponse<List<PersonalWordResponse>>> list(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam(required = false) Long listId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(wordService.list(user.getUsername(), listId, status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PersonalWordResponse>> create(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody PersonalWordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(wordService.create(user.getUsername(), request), "Đã thêm từ vựng"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PersonalWordResponse>> update(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @Valid @RequestBody PersonalWordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(wordService.update(user.getUsername(), id, request), "Đã cập nhật từ vựng"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id) {
        wordService.delete(user.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.message("Đã xóa từ vựng"));
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<PersonalWordResponse>> review(
            @AuthenticationPrincipal UserDetails user,
            @PathVariable Long id,
            @RequestParam boolean known) {
        return ResponseEntity.ok(ApiResponse.success(wordService.review(user.getUsername(), id, known)));
    }
}
