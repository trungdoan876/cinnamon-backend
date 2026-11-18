package com.cinnamon.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WordListResponse {
    private Long id;
    private String name;
    private String description;
    /** Tổng số từ trong danh sách. */
    private long wordCount;
    /** Số từ đã thuộc. */
    private long masteredCount;
}

