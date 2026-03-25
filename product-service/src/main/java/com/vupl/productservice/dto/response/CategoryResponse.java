package com.vupl.productservice.dto.response;

import com.vupl.productservice.entity.Category;
import lombok.*;

@Data
@Builder
public class CategoryResponse {
    private String id;
    private String name;
    private String slug;
    private String parentId;
    private Integer sortOrder;
    private Boolean isActive;

    public static CategoryResponse from(Category c) {
        return CategoryResponse.builder()
                .id(c.getId()).name(c.getName()).slug(c.getSlug())
                .parentId(c.getParent() != null ? c.getParent().getId() : null)
                .sortOrder(c.getSortOrder()).isActive(c.getIsActive()).build();
    }
}
