package com.vupl.notificationservice.dto.request;

import com.vupl.notificationservice.entity.PushToken.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterPushTokenRequest {
    @NotBlank
    private String deviceToken;
    @NotNull
    private Platform platform;
}
