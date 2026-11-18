package com.cinnamon.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Từ vựng cá nhân: mỗi người dùng tự thêm từ riêng để học bằng flashcard.
 * Độc lập với {@link Vocabulary} (từ vựng của bài học do giáo viên tạo).
 */
@Entity
@Table(name = "personal_words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Danh sách từ chứa flashcard này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "list_id")
    private WordList list;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(length = 100)
    private String pronunciation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String definition;

    @Column(name = "example_sentence", columnDefinition = "TEXT")
    private String exampleSentence;

    /** Đã thuộc hay chưa. */
    @Column(name = "is_mastered")
    @Builder.Default
    private Boolean mastered = false;

    /** Số lần đã ôn tập. */
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;
}

