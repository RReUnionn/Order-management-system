package com.vupl.orderservice.dto.response;

import com.vupl.orderservice.entity.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class OrderResponse {
    private String id;
    private String orderCode;
    private String userId;
    private String status;
    private String sagaStep;
    // Shipping
    private String shipRecipient;
    private String shipPhone;
    private String shipProvince;
    private String shipDistrict;
    private String shipWard;
    private String shipStreet;
    // Pricing
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String note;
    private String cancelReason;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OrderResponse from(Order o) {
        return OrderResponse.builder()
                .id(o.getId()).orderCode(o.getOrderCode()).userId(o.getUserId())
                .status(o.getStatus().name()).sagaStep(o.getSagaStep())
                .shipRecipient(o.getShipRecipient()).shipPhone(o.getShipPhone())
                .shipProvince(o.getShipProvince()).shipDistrict(o.getShipDistrict())
                .shipWard(o.getShipWard()).shipStreet(o.getShipStreet())
                .subtotal(o.getSubtotal()).shippingFee(o.getShippingFee())
                .discountAmount(o.getDiscountAmount()).totalAmount(o.getTotalAmount())
                .note(o.getNote()).cancelReason(o.getCancelReason())
                .items(o.getItems() != null
                        ? o.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList())
                        : List.of())
                .createdAt(o.getCreatedAt()).updatedAt(o.getUpdatedAt())
                .build();
    }

    @Data
    @Builder
    public static class OrderItemResponse {
        private String id;
        private String productId;
        private String productName;
        private String productSku;
        private String productImageUrl;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;

        public static OrderItemResponse from(OrderItem i) {
            return OrderItemResponse.builder()
                    .id(i.getId()).productId(i.getProductId()).productName(i.getProductName())
                    .productSku(i.getProductSku()).productImageUrl(i.getProductImageUrl())
                    .unitPrice(i.getUnitPrice()).quantity(i.getQuantity()).subtotal(i.getSubtotal())
                    .build();
        }
    }
}
