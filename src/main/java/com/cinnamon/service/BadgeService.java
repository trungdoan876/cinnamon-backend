package com.cinnamon.service;

import com.cinnamon.dto.response.BadgeResponse;
import com.cinnamon.entity.*;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final UserProgressRepository userProgressRepository;

    @PostConstruct
    public void seedInitialBadges() {
        if (badgeRepository.count() == 0) {
            List<Badge> defaultBadges = List.of(
                    Badge.builder().name("Tân Binh Xuất Sắc").description("Hoàn thành bài học đầu tiên trên Cinnamon").icon("🌱").conditionType(BadgeConditionType.LESSONS_COMPLETED).conditionValue(1).build(),
                    Badge.builder().name("Thắp Lửa Streak").description("Duy trì chuỗi học tập liên tiếp 7 ngày").icon("🔥").conditionType(BadgeConditionType.STREAK_DAYS).conditionValue(7).build(),
                    Badge.builder().name("Chiến Binh Cấp 5").description("Đạt cấp độ Level 5 trong hành trình học tập").icon("⚔️").conditionType(BadgeConditionType.LEVEL_REACHED).conditionValue(5).build(),
                    Badge.builder().name("Bậc Thầy Tích Lũy").description("Tích lũy tối thiểu 100 điểm kinh nghiệm XP").icon("👑").conditionType(BadgeConditionType.XP_REACHED).conditionValue(100).build(),
                    Badge.builder().name("Học Bá Chăm Chỉ").description("Hoàn thành từ 5 bài học trở lên").icon("📚").conditionType(BadgeConditionType.LESSONS_COMPLETED).conditionValue(5).build(),
                    Badge.builder().name("Chuỗi Siêu Cấp 30 Ngày").description("Duy trì chuỗi học tập kiên trì 30 ngày").icon("⚡").conditionType(BadgeConditionType.STREAK_DAYS).conditionValue(30).build(),
                    Badge.builder().name("Huyền Thoại Cinnamon").description("Đạt tổng cộng 500 điểm XP trên hệ thống").icon("🏆").conditionType(BadgeConditionType.XP_REACHED).conditionValue(500).build()
            );
            badgeRepository.saveAll(defaultBadges);
        }
    }

    @Transactional
    public List<Badge> evaluateBadges(User user) {
        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElse(UserXp.builder().user(user).totalXp(0).level(1).build());

        long completedLessons = userProgressRepository.countCompletedByUserId(user.getId());
        int streakDays = user.getStreakDays() != null ? user.getStreakDays() : 0;
        int totalXp = userXp.getTotalXp();
        int level = userXp.getLevel();

        List<Badge> allBadges = badgeRepository.findAll();
        List<UserBadge> currentBadges = userBadgeRepository.findByUserId(user.getId());
        Set<Long> ownedBadgeIds = currentBadges.stream()
                .map(ub -> ub.getBadge().getId())
                .collect(Collectors.toSet());

        List<Badge> newlyAwarded = new ArrayList<>();

        for (Badge badge : allBadges) {
            if (ownedBadgeIds.contains(badge.getId())) continue;

            boolean qualified = false;
            switch (badge.getConditionType()) {
                case LESSONS_COMPLETED -> qualified = completedLessons >= badge.getConditionValue();
                case STREAK_DAYS -> qualified = streakDays >= badge.getConditionValue();
                case LEVEL_REACHED -> qualified = level >= badge.getConditionValue();
                case XP_REACHED -> qualified = totalXp >= badge.getConditionValue();
            }

            if (qualified) {
                UserBadge ub = UserBadge.builder()
                        .user(user)
                        .badge(badge)
                        .earnedAt(LocalDateTime.now())
                        .build();
                userBadgeRepository.save(ub);
                newlyAwarded.add(badge);
            }
        }

        return newlyAwarded;
    }

    public List<BadgeResponse> getUserBadges(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // Trigger evaluation first
        evaluateBadges(user);

        UserXp userXp = userXpRepository.findByUserId(user.getId())
                .orElse(UserXp.builder().user(user).totalXp(0).level(1).build());

        long completedLessons = userProgressRepository.countCompletedByUserId(user.getId());
        int streakDays = user.getStreakDays() != null ? user.getStreakDays() : 0;
        int totalXp = userXp.getTotalXp();
        int level = userXp.getLevel();

        List<Badge> allBadges = badgeRepository.findAll();
        List<UserBadge> userBadges = userBadgeRepository.findByUserId(user.getId());
        Map<Long, UserBadge> badgeMap = userBadges.stream()
                .collect(Collectors.toMap(ub -> ub.getBadge().getId(), ub -> ub, (a, b) -> a));

        List<BadgeResponse> results = new ArrayList<>();

        for (Badge badge : allBadges) {
            boolean unlocked = badgeMap.containsKey(badge.getId());
            LocalDateTime earnedAt = unlocked ? badgeMap.get(badge.getId()).getEarnedAt() : null;

            int currentVal = 0;
            switch (badge.getConditionType()) {
                case LESSONS_COMPLETED -> currentVal = (int) completedLessons;
                case STREAK_DAYS -> currentVal = streakDays;
                case LEVEL_REACHED -> currentVal = level;
                case XP_REACHED -> currentVal = totalXp;
            }

            int targetVal = badge.getConditionValue();
            int percent = Math.min(100, (int) Math.round(((double) currentVal / targetVal) * 100));

            results.add(BadgeResponse.builder()
                    .id(badge.getId())
                    .name(badge.getName())
                    .description(badge.getDescription())
                    .icon(badge.getIcon())
                    .unlocked(unlocked)
                    .earnedAt(earnedAt)
                    .currentProgress(currentVal)
                    .targetProgress(targetVal)
                    .progressPercent(percent)
                    .build());
        }

        return results;
    }
}
