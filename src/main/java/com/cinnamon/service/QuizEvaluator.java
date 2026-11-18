package com.cinnamon.service;

import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Map;

/**
 * QuizEvaluator - Xử lý tính toán và chấm điểm bài kiểm tra (Quiz).
 * Đây là lớp Logic thuần túy (Pure Logic), không dính Database hay Spring Boot,
 * giúp hệ thống chạy nhanh và viết Automation Test dễ dàng.
 */
public final class QuizEvaluator {

    public static final int PASSING_SCORE_PERCENT = 60;
    public static final int DEFAULT_FAILED_XP = 5;
    public static final int DEFAULT_SUCCESS_XP = 20;

    /**
     * Lớp đóng gói kết quả sau khi chấm điểm một bài kiểm tra.
     */
    @Getter
    @Builder
    public static class Result {
        private final int totalQuestions;
        private final int correctCount;
        private final int scorePercent;
        private final boolean passed;
        private final int xpEarned;
    }

    private QuizEvaluator() {
        // Utility class
    }

    /**
     * Chấm điểm bài kiểm tra dựa trên danh sách câu hỏi và đáp án người dùng nộp.
     *
     * @param totalQuestions   tổng số câu hỏi trong bài kiểm tra
     * @param correctAnswers   bản đồ ánh xạ [idCâuHỏi -> idĐápÁnĐúng]
     * @param submittedAnswers bản đồ ánh xạ [idCâuHỏi -> idĐápÁnNgườiDùngChọn]
     * @param xpReward         điểm kinh nghiệm (XP) cấu hình thưởng cho bài quiz nếu đạt
     * @return đối tượng Result chứa: số câu đúng, tỷ lệ % điểm, đạt/không đạt (>= 60%), và số XP nhận được
     */
    public static Result evaluate(
            int totalQuestions,
            Map<Long, Long> correctAnswers,
            Map<Long, Long> submittedAnswers,
            Integer xpReward
    ) {
        if (totalQuestions <= 0) {
            int reward = (xpReward != null) ? xpReward : DEFAULT_SUCCESS_XP;
            return Result.builder()
                    .totalQuestions(0)
                    .correctCount(0)
                    .scorePercent(100)
                    .passed(true)
                    .xpEarned(reward)
                    .build();
        }

        Map<Long, Long> safeSubmitted = submittedAnswers != null ? submittedAnswers : Collections.emptyMap();
        Map<Long, Long> safeCorrect = correctAnswers != null ? correctAnswers : Collections.emptyMap();

        int correctCount = 0;
        for (Map.Entry<Long, Long> entry : safeCorrect.entrySet()) {
            Long questionId = entry.getKey();
            Long expectedOptionId = entry.getValue();
            Long userChoice = safeSubmitted.get(questionId);

            if (expectedOptionId != null && expectedOptionId.equals(userChoice)) {
                correctCount++;
            }
        }

        int scorePercent = (int) Math.round(((double) correctCount / totalQuestions) * 100);
        boolean passed = scorePercent >= PASSING_SCORE_PERCENT;
        int reward = (xpReward != null) ? xpReward : DEFAULT_SUCCESS_XP;
        int xpEarned = passed ? reward : DEFAULT_FAILED_XP;

        return Result.builder()
                .totalQuestions(totalQuestions)
                .correctCount(correctCount)
                .scorePercent(scorePercent)
                .passed(passed)
                .xpEarned(xpEarned)
                .build();
    }
}
