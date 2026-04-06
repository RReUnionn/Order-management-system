package com.vupl.inventoryservice.controller;
import com.vupl.inventoryservice.dto.request.*;
import com.vupl.inventoryservice.dto.response.*;

import com.vupl.inventoryservice.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /** Nhập kho sản phẩm */
    @PostMapping("/import")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> importStock(
            @Valid @RequestBody ImportStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Nhập kho thành công", inventoryService.importStock(request)));
    }

    /** Điều chỉnh tồn kho thủ công */
    @PatchMapping("/{inventoryId}/adjust")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<InventoryResponse>> adjustStock(
            @PathVariable String inventoryId,
            @Valid @RequestBody AdjustStockRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.adjustStock(inventoryId, request)));
    }

    /** Xem tồn kho theo sản phẩm */
    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getByProduct(
            @PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getByProductId(productId)));
    }

    /** Xem tồn kho theo sản phẩm + kho cụ thể */
    @GetMapping("/product/{productId}/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getByProductAndWarehouse(
            @PathVariable String productId,
            @PathVariable String warehouseId) {
        return ResponseEntity.ok(ApiResponse.ok(
                inventoryService.getByProductAndWarehouse(productId, warehouseId)));
    }

    /** Lịch sử nhập/xuất kho của sản phẩm */
    @GetMapping("/product/{productId}/logs")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Page<InventoryLogResponse>>> getLogs(
            @PathVariable String productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLogs(productId, pageable)));
    }

    /** Danh sách sản phẩm sắp hết hàng */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockItems()));
    }
}
