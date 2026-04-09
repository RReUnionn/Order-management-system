package com.vupl.notificationservice.event.payload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebSocketMessage {
    private String notificationId;
    private String userId;
    private String type;        // ORDER_CREATED, PAYMENT_COMPLETED, ...
    private String title;
    private String message;
    private String referenceType;
    private String referenceId;
    private LocalDateTime createdAt;
}
