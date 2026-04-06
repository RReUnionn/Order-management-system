package com.vupl.orderservice.service;
import com.vupl.orderservice.dto.request.*;
import com.vupl.orderservice.dto.response.*;
import com.vupl.orderservice.event.payload.*;
public interface OrderService {
    OrderResponse createOrder(String userId, CreateOrderRequest request);
    OrderResponse getOrderById(String orderId, String userId);
    OrderResponse getOrderByCode(String orderCode, String userId);
    PageResponse<OrderResponse> getMyOrders(String userId, String status, int page, int size);
    PageResponse<OrderResponse> getAllOrders(String status, int page, int size);
    void cancelOrder(String orderId, String userId, CancelOrderRequest request);
    OrderResponse updateStatus(String orderId, String status);

    // Saga consumers
    void handleInventoryReserved(InventoryReservedPayload payload);
    void handleInventoryReserveFailed(InventoryReserveFailedPayload payload);
    void handlePaymentCompleted(PaymentCompletedPayload payload);
    void handlePaymentFailed(PaymentFailedPayload payload);
}
