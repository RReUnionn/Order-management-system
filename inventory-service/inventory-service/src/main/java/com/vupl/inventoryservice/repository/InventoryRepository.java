package com.vupl.inventoryservice.repository;
import com.vupl.inventoryservice.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface InventoryRepository extends JpaRepository<Inventory, String> {
    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    List<Inventory> findByProductIdWithLock(@Param("productId") String productId);

    List<Inventory> findByProductId(String productId);

    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQty) <= i.reorderPoint")
    List<Inventory> findLowStockItems();
}
