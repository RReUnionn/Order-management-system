package com.vupl.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;
@Entity @Table(name="warehouses")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Warehouse {
    @Id @UuidGenerator @Column(length=36) private String id;
    @Column(nullable=false, length=100) private String name;
    @Column(length=300) private String address;
    @Column(name="is_active", nullable=false) @Builder.Default private Boolean isActive = true;
}
