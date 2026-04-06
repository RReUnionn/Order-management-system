package com.vupl.orderservice.controller;

import com.vupl.orderservice.dto.request.*;
import com.vupl.orderservice.dto.response.*;
import com.vupl.orderservice.security.JwtService;
import com.vupl.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final JwtService jwtService;

    // ── Customer endpoints ────────────────────────────────────

    /** Tạo đơn hàng mới */
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateOrderRequest request) {
        String userId = extractUserId(httpRequest);
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Đặt hàng thành công", orderService.createOrder(userId, request)));
    }

    /** Lấy danh sách đơn hàng của mình */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getMyOrders(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getMyOrders(userId, status, page, size)));
    }

    /** Xem chi tiết đơn hàng theo ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getById(
            HttpServletRequest httpRequest,
            @PathVariable String id) {
        String userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id, userId)));
    }

    /** Xem đơn hàng theo order code */
    @GetMapping("/code/{orderCode}")
    public ResponseEntity<ApiResponse<OrderResponse>> getByCode(
            HttpServletRequest httpRequest,
            @PathVariable String orderCode) {
        String userId = extractUserId(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderByCode(orderCode, userId)));
    }

    /** Hủy đơn hàng */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            HttpServletRequest httpRequest,
            @PathVariable String id,
            @Valid @RequestBody CancelOrderRequest request) {
        String userId = extractUserId(httpRequest);
        orderService.cancelOrder(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy đơn hàng", null));
    }

    // ── Admin endpoints ───────────────────────────────────────

    /** Admin xem tất cả đơn hàng */
    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(status, page, size)));
    }

    /** Admin xem đơn hàng bất kỳ theo ID */
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> adminGetById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(id, null)));
    }

    /** Admin cập nhật trạng thái đơn hàng thủ công */
    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateStatus(id, status)));
    }

    // ── Helper ────────────────────────────────────────────────

    private String extractUserId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            // Lấy email làm userId tạm thời — trong thực tế nên store userId trong token claims
            return jwtService.extractUsername(token);
        }
        throw new RuntimeException("Token không hợp lệ");
    }
}
