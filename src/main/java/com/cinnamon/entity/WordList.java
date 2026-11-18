package com.cinnamon.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Danh sách từ (bộ flashcard) của người dùng, ví dụ "IELTS Unit 1", "Từ vựng du lịch".
 * Mỗi danh sách chứa nhiều {@link PersonalWord}.
 */
@Entity
@Table(name = "word_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WordList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}

