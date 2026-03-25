package com.vupl.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategoryRequest {
    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100)
    private String name;
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 120)
    private String slug;
    private String parentId;
    private Integer sortOrder = 0;
    private Boolean isActive = true;
}
