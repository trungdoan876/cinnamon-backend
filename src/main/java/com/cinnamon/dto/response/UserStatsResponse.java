package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserStatsResponse {
    private Long userId;
    private String fullName;
    private String email;
    private int totalXp;
    private int weeklyXp;
    private int level;
    private int xpToNextLevel;
    private int streakDays;
    private long completedLessonsCount;
}
