package com.vupl.paymentservice.controller;

import com.vupl.paymentservice.dto.request.*;
import com.vupl.paymentservice.dto.response.*;
import com.vupl.paymentservice.security.JwtService;
import com.vupl.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final JwtService jwtService;

    // ── Customer endpoints ────────────────────────────────────

    /**
     * Khởi tạo thanh toán cho đơn hàng — chọn method và nhận payment URL
     * POST /api/payments/orders/{orderId}/initiate
     */
    @PostMapping("/orders/{orderId}/initiate")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiatePayment(
            HttpServletRequest httpRequest,
            @PathVariable String orderId,
            @Valid @RequestBody InitiatePaymentRequest request) {
        String userId = extractEmail(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                "Link thanh toán đã được tạo",
                paymentService.initiatePayment(orderId, userId, request)));
    }

    /**
     * Xem trạng thái payment của đơn hàng
     * GET /api/payments/orders/{orderId}
     */
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(
            HttpServletRequest httpRequest,
            @PathVariable String orderId) {
        String userId = extractEmail(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByOrderId(orderId, userId)));
    }

    /**
     * Xem lịch sử events của payment (audit log)
     * GET /api/payments/{paymentId}/events
     */
    @GetMapping("/{paymentId}/events")
    public ResponseEntity<ApiResponse<List<PaymentEventResponse>>> getEvents(
            HttpServletRequest httpRequest,
            @PathVariable String paymentId) {
        String userId = extractEmail(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentEvents(paymentId, userId)));
    }

    /**
     * Yêu cầu hoàn tiền
     * POST /api/payments/{paymentId}/refund
     */
    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            HttpServletRequest httpRequest,
            @PathVariable String paymentId,
            @Valid @RequestBody RefundRequest request) {
        String userId = extractEmail(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(
                "Yêu cầu hoàn tiền đã được ghi nhận",
                paymentService.requestRefund(paymentId, userId, request)));
    }

    /**
     * Xem danh sách refund của payment
     * GET /api/payments/{paymentId}/refunds
     */
    @GetMapping("/{paymentId}/refunds")
    public ResponseEntity<ApiResponse<List<RefundResponse>>> getRefunds(
            HttpServletRequest httpRequest,
            @PathVariable String paymentId) {
        String userId = extractEmail(httpRequest);
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getRefunds(paymentId, userId)));
    }

    // ── Mock Gateway endpoints (dùng khi dev/test) ────────────

    /**
     * Simulate URL redirect tới gateway — hiển thị trang thanh toán giả lập
     * GET /api/payments/mock-gateway?orderId=...&amount=...&method=...
     */
    @GetMapping("/mock-gateway")
    public ResponseEntity<String> mockGatewayPage(
            @RequestParam String orderId,
            @RequestParam String amount,
            @RequestParam String method) {
        String html = """
            <html><body style="font-family:sans-serif;max-width:500px;margin:50px auto;text-align:center">
            <h2>Mock Payment Gateway</h2>
            <p>Order: <b>%s</b></p>
            <p>Amount: <b>%s VND</b></p>
            <p>Method: <b>%s</b></p>
            <form method="POST" action="/api/payments/mock-callback">
              <input type="hidden" name="orderId" value="%s"/>
              <input type="hidden" name="gatewayRef" value="TXN-%s"/>
              <input type="hidden" name="success" value="true"/>
              <button type="submit" style="background:green;color:white;padding:10px 24px;border:none;border-radius:4px;cursor:pointer;font-size:16px">
                ✅ Thanh toán thành công
              </button>
            </form>
            <br/>
            <form method="POST" action="/api/payments/mock-callback">
              <input type="hidden" name="orderId" value="%s"/>
              <input type="hidden" name="gatewayRef" value="TXN-FAIL-%s"/>
              <input type="hidden" name="success" value="false"/>
              <input type="hidden" name="failReason" value="Không đủ số dư"/>
              <button type="submit" style="background:red;color:white;padding:10px 24px;border:none;border-radius:4px;cursor:pointer;font-size:16px">
                ❌ Thanh toán thất bại
              </button>
            </form>
            </body></html>
            """.formatted(orderId, amount, method,
                orderId, System.currentTimeMillis(),
                orderId, System.currentTimeMillis());
        return ResponseEntity.ok().header("Content-Type", "text/html;charset=UTF-8").body(html);
    }

    /**
     * Nhận callback từ mock gateway — giả lập webhook
     * POST /api/payments/mock-callback
     */
    @PostMapping("/mock-callback")
    public ResponseEntity<ApiResponse<Void>> mockCallback(
            @RequestParam String orderId,
            @RequestParam String gatewayRef,
            @RequestParam(defaultValue = "true") boolean success,
            @RequestParam(required = false) String failReason) {
        MockGatewayCallbackRequest request = new MockGatewayCallbackRequest();
        request.setOrderId(orderId);
        request.setGatewayRef(gatewayRef);
        request.setSuccess(success);
        request.setFailReason(failReason);
        paymentService.handleGatewayCallback(request);
        String msg = success ? "Thanh toán thành công" : "Thanh toán thất bại: " + failReason;
        return ResponseEntity.ok(ApiResponse.ok(msg, null));
    }

    /**
     * Nhận webhook JSON từ gateway thật (VNPay, Momo...)
     * POST /api/payments/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @Valid @RequestBody MockGatewayCallbackRequest request) {
        paymentService.handleGatewayCallback(request);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed", null));
    }

    // ── Admin endpoints ───────────────────────────────────────

    /**
     * Admin xem payments theo trạng thái
     * GET /api/payments/admin?status=PENDING
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getByStatus(
            @RequestParam(defaultValue = "PENDING") String status) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentsByStatus(status)));
    }

    /**
     * Admin xem payment bất kỳ theo ID
     * GET /api/payments/admin/{paymentId}
     */
    @GetMapping("/admin/{paymentId}/events")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<PaymentEventResponse>>> adminGetEvents(
            @PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getPaymentEvents(paymentId, null)));
    }

    // ── Helper ────────────────────────────────────────────────

    private String extractEmail(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer "))
            return jwtService.extractUsername(header.substring(7));
        throw new RuntimeException("Token không hợp lệ");
    }
}
