package com.vupl.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;
    @Column(name = "product_sku", nullable = false, length = 60)
    private String productSku;
    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;
    @Column(nullable = false)
    private Integer quantity;
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
}
