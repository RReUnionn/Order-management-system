package com.vupl.productservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ProductRequest {
    @NotBlank(message = "SKU không được để trống")
    @Size(max = 60)
    private String sku;
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200)
    private String name;
    @NotBlank(message = "Slug không được để trống")
    @Size(max = 220)
    private String slug;
    private String description;
    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;
    private BigDecimal salePrice;
    @NotBlank(message = "Danh mục không được để trống")
    private String categoryId;
    private String brand;
    private Integer weightGram;
    private String status = "DRAFT";
    private List<String> imageUrls;
    private Map<String, String> attributes;
}
