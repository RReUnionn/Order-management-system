package com.vupl.inventoryservice.repository;

import com.vupl.inventoryservice.entity.InventoryLog;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    Page<InventoryLog> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    List<InventoryLog> findByReferenceIdOrderByCreatedAtAsc(String referenceId);
}
