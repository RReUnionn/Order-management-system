package com.vupl.inventoryservice.service;

import com.vupl.inventoryservice.dto.request.*;
import com.vupl.inventoryservice.dto.response.*;
import com.vupl.inventoryservice.event.payload.OrderCreatedPayload;
import com.vupl.inventoryservice.event.payload.PaymentFailedPayload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    InventoryResponse importStock(ImportStockRequest request);

    InventoryResponse adjustStock(String inventoryId, AdjustStockRequest request);

    List<InventoryResponse> getByProductId(String productId);

    InventoryResponse getByProductAndWarehouse(String productId, String warehouseId);

    Page<InventoryLogResponse> getLogs(String productId, Pageable pageable);

    List<InventoryResponse> getLowStockItems();

    // Saga steps
    void handleOrderCreated(OrderCreatedPayload payload);

    void handlePaymentFailed(PaymentFailedPayload payload);
}
