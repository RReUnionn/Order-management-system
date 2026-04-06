package com.vupl.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @UuidGenerator
    @Column(length = 36)
    private String id;

    @Column(name = "order_code", nullable = false, unique = true, length = 30)
    private String orderCode;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // Shipping address snapshot
    @Column(name = "ship_recipient", nullable = false, length = 100)
    private String shipRecipient;
    @Column(name = "ship_phone", nullable = false, length = 20)
    private String shipPhone;
    @Column(name = "ship_province", nullable = false, length = 60)
    private String shipProvince;
    @Column(name = "ship_district", nullable = false, length = 60)
    private String shipDistrict;
    @Column(name = "ship_ward", nullable = false, length = 60)
    private String shipWard;
    @Column(name = "ship_street", nullable = false, length = 200)
    private String shipStreet;

    // Pricing
    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    // Saga tracking
    @Column(name = "saga_step", length = 60)
    private String sagaStep;

    @Column(length = 500)
    private String note;
    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStatusHistory> statusHistory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum OrderStatus {
        PENDING, CONFIRMED, PAID, SHIPPING, DELIVERED, CANCELLED, REFUNDED
    }
}
