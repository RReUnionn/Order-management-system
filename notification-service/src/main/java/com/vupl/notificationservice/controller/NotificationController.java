package com.vupl.notificationservice.controller;

import com.vupl.notificationservice.dto.request.RegisterPushTokenRequest;
import com.vupl.notificationservice.dto.response.*;
import com.vupl.notificationservice.security.JwtService;
import com.vupl.notificationservice.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtService jwtService;

    // ── REST endpoints ────────────────────────────────────────

    /**
     * Lấy danh sách notifications của mình (có phân trang)
     * GET /api/notifications/my?page=0&size=20
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getMyNotifications(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = extractEmail(httpRequest);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(notificationService.getMyNotifications(userId, pageable)));
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     * GET /api/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(HttpServletRequest httpRequest) {
        String userId = extractEmail(httpRequest);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count)));
    }

    /**
     * Đánh dấu một notification đã đọc
     * PATCH /api/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            HttpServletRequest httpRequest,
            @PathVariable String id) {
        String userId = extractEmail(httpRequest);
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu đã đọc", null));
    }

    /**
     * Đánh dấu tất cả notifications đã đọc
     * PATCH /api/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest httpRequest) {
        String userId = extractEmail(httpRequest);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã đánh dấu tất cả đã đọc", null));
    }

    /**
     * Đăng ký push token (FCM cho mobile)
     * POST /api/notifications/push-token
     */
    @PostMapping("/push-token")
    public ResponseEntity<ApiResponse<Void>> registerPushToken(
            HttpServletRequest httpRequest,
            @Valid @RequestBody RegisterPushTokenRequest request) {
        String userId = extractEmail(httpRequest);
        notificationService.registerPushToken(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký push token thành công", null));
    }

    /**
     * Hủy đăng ký push token
     * DELETE /api/notifications/push-token/{token}
     */
    @DeleteMapping("/push-token/{token}")
    public ResponseEntity<Void> deregisterPushToken(@PathVariable String token) {
        notificationService.deregisterPushToken(token);
        return ResponseEntity.noContent().build();
    }

    // ── Helper ────────────────────────────────────────────────

    private String extractEmail(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
            return jwtService.extractUsername(header.substring(7));
        throw new RuntimeException("Token không hợp lệ");
    }
}
