package com.cinnamon.controller;

import com.cinnamon.dto.response.ApiResponse;
import com.cinnamon.dto.response.LeaderboardEntryResponse;
import com.cinnamon.dto.response.UserStatsResponse;
import com.cinnamon.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/leaderboard/weekly")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboardWeekly(
            @RequestParam(defaultValue = "10") int limit) {
        List<LeaderboardEntryResponse> list = gamificationService.getLeaderboardWeekly(limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/leaderboard/all-time")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> getLeaderboardAllTime(
            @RequestParam(defaultValue = "10") int limit) {
        List<LeaderboardEntryResponse> list = gamificationService.getLeaderboardAllTime(limit);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/users/me/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        UserStatsResponse stats = gamificationService.getUserStats(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
