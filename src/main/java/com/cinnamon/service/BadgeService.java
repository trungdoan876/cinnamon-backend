package com.cinnamon.service;

import com.cinnamon.dto.response.BadgeResponse;
import com.cinnamon.entity.*;
import com.cinnamon.enums.BadgeConditionType;
import com.cinnamon.exception.ResourceNotFoundException;
import com.cinnamon.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BadgeService - Dịch vụ quản lý hệ thống Huy hiệu (Badges) và phần thưởng thành tích học tập.
 * Đảm nhiệm:
 * - Khởi tạo danh mục huy hiệu mặc định của hệ thống khi khởi động ứng dụng (Seed Data).
 * - Tự động quét điều kiện và trao tặng huy hiệu mới khi học viên đạt các mốc thành tích.
 * - Tính toán thanh tiến độ hoàn thành (%) cho từng huy hiệu để hiển thị trực quan lên giao diện.
 */
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final UserXpRepository userXpRepository;
    private final UserProgressRepository userProgressRepository;

    /**
     * Tự động khởi tạo 7 huy hiệu mặc định nếu trong database chưa có dữ liệu huy hiệu nào.
     * Chạy tự động sau khi Spring Bean được khởi tạo (@PostConstruct).
     */
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

    /**
     * Quét và đối chiếu các tiêu chí thành tích của người dùng để mở khóa huy hiệu mới.
     * Các tiêu chí kiểm tra gồm:
     * - LESSONS_COMPLETED: Số lượng bài học đã hoàn thành.
     * - STREAK_DAYS: Số ngày đăng nhập học liên tục.
     * - LEVEL_REACHED: Cấp độ level hiện tại.
     * - XP_REACHED: Tổng điểm kinh nghiệm XP đạt được.
     *
     * @param user đối tượng người dùng cần kiểm tra
     * @return danh sách các Huy hiệu vừa mới được mở khóa thành công
     */
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
            // Nếu học viên đã có huy hiệu này rồi thì bỏ qua
            if (ownedBadgeIds.contains(badge.getId())) continue;

            boolean qualified = false;
            switch (badge.getConditionType()) {
                case LESSONS_COMPLETED -> qualified = completedLessons >= badge.getConditionValue();
                case STREAK_DAYS -> qualified = streakDays >= badge.getConditionValue();
                case LEVEL_REACHED -> qualified = level >= badge.getConditionValue();
                case XP_REACHED -> qualified = totalXp >= badge.getConditionValue();
            }

            // Nếu đủ điều kiện, lưu vào bảng user_badges và thêm vào danh sách vừa mở khóa
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

    /**
     * Lấy toàn bộ danh sách huy hiệu hệ thống kèm trạng thái đã đạt được hay chưa và tiến trình % của người dùng.
     * Trước khi lấy, hệ thống tự động kích hoạt hàm evaluateBadges để mở khóa ngay nếu người dùng vừa đạt điều kiện.
     *
     * @param email email của người dùng
     * @return Danh sách BadgeResponse kèm progressPercent (0% - 100%)
     */
    public List<BadgeResponse> getUserBadges(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        // Kích hoạt quét thành tích trước khi trả về dữ liệu
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
