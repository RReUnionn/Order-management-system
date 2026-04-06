package com.vupl.orderservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateOrderRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    private String shipRecipient;
    @NotBlank
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "SĐT không hợp lệ")
    private String shipPhone;
    @NotBlank
    private String shipProvince;
    @NotBlank
    private String shipDistrict;
    @NotBlank
    private String shipWard;
    @NotBlank
    private String shipStreet;
    private String note;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 sản phẩm")
    @Valid
    private List<OrderItemRequest> items;

    @Data
    public static class OrderItemRequest {
        @NotBlank(message = "ProductId không được để trống")
        private String productId;
        @NotBlank
        private String productName;
        @NotBlank
        private String productSku;
        private String productImageUrl;
        @NotNull
        @DecimalMin("0.01")
        private java.math.BigDecimal unitPrice;
        @NotNull
        @Min(1)
        private Integer quantity;
    }
}
