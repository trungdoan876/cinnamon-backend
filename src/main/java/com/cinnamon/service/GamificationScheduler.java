package com.cinnamon.service;

import com.cinnamon.repository.UserXpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationScheduler {

    private final UserXpRepository userXpRepository;

    /**
     * Tự động reset weekly XP vào 00:00 sáng Thứ Hai hàng tuần (theo giờ VN)
     */
    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void resetWeeklyLeaderboard() {
        log.info("Bắt đầu reset điểm Weekly XP cho bảng xếp hạng tuần mới...");
        userXpRepository.resetWeeklyXp();
        log.info("Hoàn tất reset Weekly XP!");
    }
}
