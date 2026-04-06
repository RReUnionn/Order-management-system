package com.vupl.paymentservice.service;

import com.vupl.paymentservice.dto.request.*;
import com.vupl.paymentservice.dto.response.*;
import com.vupl.paymentservice.event.payload.OrderConfirmedPayload;

import java.util.List;

public interface PaymentService {
    // Saga consumer
    void handleOrderConfirmed(OrderConfirmedPayload payload);

    // Customer
    PaymentResponse initiatePayment(String orderId, String userId, InitiatePaymentRequest request);

    PaymentResponse getByOrderId(String orderId, String userId);

    List<PaymentEventResponse> getPaymentEvents(String paymentId, String userId);

    // Gateway callback (webhook)
    void handleGatewayCallback(MockGatewayCallbackRequest request);

    // Refund
    RefundResponse requestRefund(String paymentId, String userId, RefundRequest request);

    List<RefundResponse> getRefunds(String paymentId, String userId);

    // Admin
    List<PaymentResponse> getPaymentsByStatus(String status);
}
