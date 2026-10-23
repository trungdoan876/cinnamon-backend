package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BadgeResponse {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private boolean unlocked;
    private LocalDateTime earnedAt;
    private int currentProgress;
    private int targetProgress;
    private int progressPercent;
}
