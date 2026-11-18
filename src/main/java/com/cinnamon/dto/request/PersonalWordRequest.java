package com.cinnamon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonalWordRequest {

    @NotNull(message = "Vui lòng chọn danh sách từ")
    private Long listId;

    @NotBlank(message = "Từ vựng không được để trống")
    @Size(max = 100, message = "Từ vựng tối đa 100 ký tự")
    private String word;

    @Size(max = 100, message = "Phiên âm tối đa 100 ký tự")
    private String pronunciation;

    @NotBlank(message = "Nghĩa không được để trống")
    private String definition;

    private String exampleSentence;
}

