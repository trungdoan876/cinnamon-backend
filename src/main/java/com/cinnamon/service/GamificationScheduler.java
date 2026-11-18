package com.cinnamon.service;

import com.cinnamon.repository.UserXpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GamificationScheduler - Tác vụ định kỳ chạy ngầm (Cron Job) cho hệ thống Game hóa.
 * Đảm nhiệm việc làm mới bảng xếp hạng đua top hàng tuần theo thời gian thực.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationScheduler {

    private final UserXpRepository userXpRepository;

    /**
     * Tác vụ tự động reset điểm Weekly XP về 0 vào đúng 00:00 sáng Thứ Hai hàng tuần (múi giờ Việt Nam Asia/Ho_Chi_Minh).
     * Điểm Total XP trọn đời của học viên vẫn được giữ nguyên không thay đổi.
     */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetWeeklyLeaderboard() {
        log.info("Bắt đầu reset điểm Weekly XP cho bảng xếp hạng tuần mới...");
        userXpRepository.resetWeeklyXp();
        log.info("Hoàn tất reset Weekly XP!");
    }
}
