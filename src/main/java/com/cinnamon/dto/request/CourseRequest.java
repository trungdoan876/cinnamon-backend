package com.cinnamon.dto.request;

import com.cinnamon.entity.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseRequest {

    @NotBlank(message = "Tên khóa học không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Cấp độ không được để trống")
    private CourseLevel level;

    private String thumbnailUrl;

    private Boolean isPublished = true;
}
