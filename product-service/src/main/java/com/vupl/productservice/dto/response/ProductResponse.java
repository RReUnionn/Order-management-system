package com.vupl.productservice.dto.response;

import com.vupl.productservice.entity.Product;
import com.vupl.productservice.entity.ProductAttribute;
import com.vupl.productservice.entity.ProductImage;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Data
@Builder
public class ProductResponse {
    private String id;
    private String sku;
    private String name;
    private String slug;
    private String description;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String categoryId;
    private String categoryName;
    private String brand;
    private Integer weightGram;
    private String status;
    private List<String> imageUrls;
    private Map<String, String> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductResponse from(Product p) {
        List<String> images = p.getImages() != null
                ? p.getImages().stream().sorted(Comparator.comparing(ProductImage::getSortOrder))
                .map(ProductImage::getUrl).collect(Collectors.toList())
                : List.of();
        Map<String, String> attrs = p.getAttributes() != null
                ? p.getAttributes().stream().collect(Collectors.toMap(ProductAttribute::getAttrKey, ProductAttribute::getAttrValue))
                : Map.of();
        return ProductResponse.builder()
                .id(p.getId()).sku(p.getSku()).name(p.getName()).slug(p.getSlug())
                .description(p.getDescription()).price(p.getPrice()).salePrice(p.getSalePrice())
                .categoryId(p.getCategory().getId()).categoryName(p.getCategory().getName())
                .brand(p.getBrand()).weightGram(p.getWeightGram()).status(p.getStatus().name())
                .imageUrls(images).attributes(attrs)
                .createdAt(p.getCreatedAt()).updatedAt(p.getUpdatedAt()).build();
    }
}
