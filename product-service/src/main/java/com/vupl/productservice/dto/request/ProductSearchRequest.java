package com.vupl.productservice.dto.request;

import lombok.Data;
import java.math.BigDecimal;
@Data
public class ProductSearchRequest {
    private String keyword;
    private String categoryId;
    private String status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortDir = "desc";
}
