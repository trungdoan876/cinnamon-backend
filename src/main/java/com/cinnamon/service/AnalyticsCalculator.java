package com.cinnamon.service;

/**
 * AnalyticsCalculator - Cung cấp các hàm toán học hỗ trợ báo cáo và thống kê giáo viên/quản trị.
 * Tách biệt thuật toán làm tròn số và tính tỷ lệ để kiểm thử độc lập.
 */
public final class AnalyticsCalculator {

    private AnalyticsCalculator() {
        // Utility class
    }

    /**
     * Làm tròn điểm số hoặc giá trị trung bình đến 1 chữ số thập phân (ví dụ: 8.46 -> 8.5).
     * Tự động xử lý trường hợp giá trị null, NaN hoặc vô hạn -> trả về 0.0 an toàn.
     *
     * @param score điểm số thô
     * @return điểm số đã được làm tròn an toàn
     */
    public static double roundScore(Double score) {
        if (score == null || Double.isNaN(score) || Double.isInfinite(score)) {
            return 0.0;
        }
        return Math.round(score * 10.0) / 10.0;
    }

    /**
     * Tính toán tỷ lệ phần trăm hoàn thành (completion percentage).
     *
     * @param completed số lượng mục đã hoàn thành
     * @param total     tổng số lượng mục
     * @return tỷ lệ phần trăm (từ 0.0 đến 100.0) đã làm tròn
     */
    public static double calculateCompletionRate(long completed, long total) {
        if (total <= 0) {
            return 0.0;
        }
        double rate = ((double) completed / total) * 100.0;
        return roundScore(rate);
    }
}
