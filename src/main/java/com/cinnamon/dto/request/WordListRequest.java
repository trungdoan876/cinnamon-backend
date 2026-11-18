package com.cinnamon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WordListRequest {

    @NotBlank(message = "Tên danh sách không được để trống")
    @Size(max = 100, message = "Tên danh sách tối đa 100 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
    private String description;
}

