package com.vupl.inventoryservice.repository;
import com.vupl.inventoryservice.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    List<Warehouse> findByIsActiveTrue();
}
