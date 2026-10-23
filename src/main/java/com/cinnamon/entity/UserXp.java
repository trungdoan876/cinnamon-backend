package com.cinnamon.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_xp")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserXp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "total_xp")
    @Builder.Default
    private Integer totalXp = 0;

    @Column(name = "level")
    @Builder.Default
    private Integer level = 1;

    @Column(name = "weekly_xp")
    @Builder.Default
    private Integer weeklyXp = 0;

    public void addXp(int xp) {
        this.totalXp += xp;
        this.weeklyXp += xp;
        this.level = calculateLevel(this.totalXp);
    }

    private int calculateLevel(int totalXp) {
        // Level up mỗi 100 XP
        return Math.max(1, totalXp / 100 + 1);
    }
}
