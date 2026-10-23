package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryResponse {
    private int rank;
    private Long userId;
    private String username;
    private String fullName;
    private String avatarUrl;
    private int xp;
    private int level;
    private int streakDays;
}
