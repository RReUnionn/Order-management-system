package com.vupl.userservice.dto.response;

import com.vupl.userservice.entity.UserAddress;
import lombok.*;

@Data @Builder
public class AddressResponse {
    private String id;
    private String recipientName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String street;
    private String addressType;
    private Boolean isDefault;

    public static AddressResponse from(UserAddress a) {
        return AddressResponse.builder()
                .id(a.getId()).recipientName(a.getRecipientName()).phone(a.getPhone())
                .province(a.getProvince()).district(a.getDistrict()).ward(a.getWard())
                .street(a.getStreet()).addressType(a.getAddressType().name())
                .isDefault(a.getIsDefault()).build();
    }
}
