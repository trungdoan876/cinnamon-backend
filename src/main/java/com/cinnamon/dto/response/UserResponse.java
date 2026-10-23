package com.cinnamon.dto.response;

import com.cinnamon.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private Boolean isActive;
    private Integer streakDays;
    private LocalDate lastLoginDate;
    private LocalDateTime createdAt;
}
