package com.vupl.userservice.dto.request;

import com.vupl.userservice.entity.UserAddress;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank private String province;
    @NotBlank private String district;
    @NotBlank private String ward;
    @NotBlank private String street;

    private UserAddress.AddressType addressType = UserAddress.AddressType.HOME;
    private Boolean isDefault = false;
}
