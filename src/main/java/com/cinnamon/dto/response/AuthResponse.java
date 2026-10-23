package com.cinnamon.dto.response;

import com.cinnamon.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private Role role;
    private Integer streakDays;
}
