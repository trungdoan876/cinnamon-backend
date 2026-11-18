package com.cinnamon.service;

import com.cinnamon.dto.response.LeaderboardEntryResponse;
import com.cinnamon.dto.response.UserStatsResponse;
import com.cinnamon.entity.User;
import com.cinnamon.entity.UserXp;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.UserProgressRepository;
import com.cinnamon.repository.UserRepository;
import com.cinnamon.repository.UserXpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * GamificationService - Dịch vụ xử lý hệ thống Game hóa học tập (Gamification).
 * Cung cấp:
 * - Bảng xếp hạng học viên chăm chỉ trong tuần (Weekly Leaderboard).
 * - Bảng xếp hạng học viên xuất sắc nhất mọi thời đại (All-time Leaderboard).
 * - Xem thống kê chỉ số học tập cá nhân (Level, XP, Streak, số bài học hoàn thành).
 */
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserXpRepository userXpRepository;
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;

    /**
     * Lấy danh sách bảng xếp hạng theo điểm kinh nghiệm tích lũy trong tuần (Weekly XP).
     *
     * @param limit số lượng học viên top đầu muốn lấy (ví dụ: top 10 hoặc top 50)
     * @return Danh sách LeaderboardEntryResponse đã được xếp thứ hạng từ 1 đến N
     */
    public List<LeaderboardEntryResponse> getLeaderboardWeekly(int limit) {
        List<UserXp> list = userXpRepository.findAllByOrderByWeeklyXpDesc(PageRequest.of(0, limit));
        return mapToLeaderboard(list, true);
    }

    /**
     * Lấy danh sách bảng xếp hạng theo tổng điểm kinh nghiệm trọn đời (Total XP).
     *
     * @param limit số lượng học viên top đầu muốn lấy
     * @return Danh sách LeaderboardEntryResponse đã được xếp thứ hạng từ 1 đến N
     */
    public List<LeaderboardEntryResponse> getLeaderboardAllTime(int limit) {
        List<UserXp> list = userXpRepository.findAllByOrderByTotalXpDesc(PageRequest.of(0, limit));
        return mapToLeaderboard(list, false);
    }

    /**
     * Lấy thông tin thống kê thành tích học tập chi tiết của một người dùng.
     * Tận dụng phương thức domain userXp.getXpToNextLevel() để lấy số điểm cần để thăng cấp.
     *
     * @param email email của người dùng
     * @return UserStatsResponse chứa Level, Total XP, Weekly XP, XP cần lên cấp, Streak, số bài đã xong
     */
    public UserStatsResponse getUserStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElse(UserXp.builder().user(user).totalXp(0).weeklyXp(0).level(1).build());

        long completedCount = userProgressRepository.countCompletedByUserId(user.getId());

        int totalXp = userXp.getTotalXp() != null ? userXp.getTotalXp() : 0;
        int xpToNextLevel = calculateXpToNextLevel(totalXp);

        return UserStatsResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .totalXp(userXp.getTotalXp())
                .weeklyXp(userXp.getWeeklyXp())
                .level(userXp.getLevel())
                .xpToNextLevel(xpToNextLevel)
                .streakDays(user.getStreakDays() != null ? user.getStreakDays() : 1)
                .completedLessonsCount(completedCount)
                .build();
    }

    public void addXp(UserXp userXp, int xp) {
        int currentTotal = (userXp.getTotalXp() != null ? userXp.getTotalXp() : 0) + xp;
        int currentWeekly = (userXp.getWeeklyXp() != null ? userXp.getWeeklyXp() : 0) + xp;
        userXp.setTotalXp(currentTotal);
        userXp.setWeeklyXp(currentWeekly);
        userXp.setLevel(calculateLevel(currentTotal));
    }

    public int calculateLevel(int totalXp) {
        return Math.max(1, totalXp / 100 + 1);
    }

    public int calculateXpToNextLevel(int totalXp) {
        int progress = totalXp % 100;
        return 100 - progress;
    }

    /**
     * Chuyển đổi danh sách thực thể UserXp sang DTO hiển thị bảng xếp hạng kèm số thứ tự (Rank).
     *
     * @param list   danh sách UserXp từ database
     * @param weekly true nếu lấy weekly XP, false nếu lấy total XP
     */
    private List<LeaderboardEntryResponse> mapToLeaderboard(List<UserXp> list, boolean weekly) {
        List<LeaderboardEntryResponse> result = new ArrayList<>();
        int rank = 1;
        for (UserXp xp : list) {
            User u = xp.getUser();
            result.add(LeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .userId(u.getId())
                    .username(u.getRealUsername())
                    .fullName(u.getFullName())
                    .avatarUrl(u.getAvatarUrl())
                    .xp(weekly ? xp.getWeeklyXp() : xp.getTotalXp())
                    .level(xp.getLevel())
                    .streakDays(u.getStreakDays() != null ? u.getStreakDays() : 0)
                    .build());
        }
        return result;
    }
}
