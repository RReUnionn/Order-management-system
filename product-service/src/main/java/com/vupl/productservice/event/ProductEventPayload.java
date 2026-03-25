package com.vupl.productservice.event;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEventPayload {
    private String productId;
    private String sku;
    private String name;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String categoryId;
    private String status;
    private String eventType;
}
