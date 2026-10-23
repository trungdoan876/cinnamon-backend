package com.cinnamon.controller;

import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.TeacherAnalyticsResponse;
import com.cinnamon.service.TeacherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherAnalyticsService teacherAnalyticsService;

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeacherAnalyticsResponse>> getAnalytics(
            @AuthenticationPrincipal UserDetails userDetails) {
        TeacherAnalyticsResponse analytics = teacherAnalyticsService.getAnalytics(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(analytics));
    }
}
