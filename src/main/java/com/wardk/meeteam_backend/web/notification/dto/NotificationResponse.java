package com.wardk.meeteam_backend.web.notification.dto;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.web.notification.payload.NotificationPayload;
import lombok.Data;

import java.time.LocalDate;

@Data
@JsonPropertyOrder({
        "id",
        "type",
        "message",
        "isRead",
        "createdAt",
        "applicationId",
        "payload"
})
public class NotificationResponse {

    private Long id;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private LocalDate createdAt;
    private Long applicationId; // 지원서 자체 ID
    private NotificationPayload payload;


    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.type = notification.getType();
        this.isRead = notification.isRead();
        this.createdAt = LocalDate.from(notification.getCreatedAt());
        this.applicationId = notification.getApplicationId();
        this.payload = new NotificationPayload(notification.getProject(), notification.getReceiver());
    }

}
