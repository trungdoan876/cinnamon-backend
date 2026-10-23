package com.cinnamon.controller;

import com.cinnamon.dto.response.AdminStatsResponse;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.entity.Role;
import com.cinnamon.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        AdminStatsResponse stats = adminService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 15) Pageable pageable) {
        Page<UserResponse> users = adminService.searchUsers(role, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String roleStr = body.get("role");
        if (roleStr == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Thiếu trường 'role'"));
        }
        Role newRole = Role.valueOf(roleStr.toUpperCase());
        UserResponse updated = adminService.updateUserRole(id, newRole);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật quyền thành công", updated));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "Thiếu trường 'isActive'"));
        }
        UserResponse updated = adminService.updateUserStatus(id, isActive);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
    }
}
