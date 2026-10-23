package com.cinnamon.controller;

import com.cinnamon.dto.request.ChangePasswordRequest;
import com.cinnamon.dto.request.UpdateProfileRequest;
import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.UserResponse;
import com.cinnamon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.cinnamon.service.BadgeService badgeService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserResponse profile = userService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @GetMapping("/me/badges")
    public ResponseEntity<ApiResponse<java.util.List<com.cinnamon.dto.response.BadgeResponse>>> getMyBadges(
            @AuthenticationPrincipal UserDetails userDetails) {
        java.util.List<com.cinnamon.dto.response.BadgeResponse> badges = badgeService.getUserBadges(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(badges));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse updated = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin thành công", updated));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.message("Đổi mật khẩu thành công"));
    }
}
