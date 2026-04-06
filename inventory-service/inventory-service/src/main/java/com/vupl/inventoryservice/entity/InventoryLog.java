package com.vupl.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "inventory_id", nullable = false, length = 36)
    private String inventoryId;
    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
    @Column(nullable = false)
    private Integer delta;
    @Column(name = "qty_after", nullable = false)
    private Integer qtyAfter;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LogReason reason;
    @Column(name = "reference_id", length = 36)
    private String referenceId;
    @Column(length = 300)
    private String note;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum LogReason {IMPORT, RESERVE, RELEASE, CONFIRM_DEDUCT, ADJUSTMENT, RETURN}
}
