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

@Service
@RequiredArgsConstructor
public class GamificationService {

    private final UserXpRepository userXpRepository;
    private final UserRepository userRepository;
    private final UserProgressRepository userProgressRepository;

    public List<LeaderboardEntryResponse> getLeaderboardWeekly(int limit) {
        List<UserXp> list = userXpRepository.findAllByOrderByWeeklyXpDesc(PageRequest.of(0, limit));
        return mapToLeaderboard(list, true);
    }

    public List<LeaderboardEntryResponse> getLeaderboardAllTime(int limit) {
        List<UserXp> list = userXpRepository.findAllByOrderByTotalXpDesc(PageRequest.of(0, limit));
        return mapToLeaderboard(list, false);
    }

    public UserStatsResponse getUserStats(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElse(UserXp.builder().user(user).totalXp(0).weeklyXp(0).level(1).build());

        long completedCount = userProgressRepository.countCompletedByUserId(user.getId());

        int currentLevel = userXp.getLevel();
        int xpForNextLevel = currentLevel * 100;
        int currentProgressInLevel = userXp.getTotalXp() % 100;
        int xpToNextLevel = 100 - currentProgressInLevel;

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
