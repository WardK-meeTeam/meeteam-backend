package com.wardk.meeteam_backend.domain.notification;

import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@Getter
@Builder
public class NotificationPayload {


    private Long id;           // notification PK (optional)
    private String type;       // e.g. PROJECT_APPLY
    private String message;
    private Long projectId;
    private boolean read;
    private LocalDateTime createdAt;

    public static NotificationPayload from(Notification n) {
        return NotificationPayload.builder()
                .id(n.getId())
                .type(n.getType().name())
                .message(n.getMessage())
                .projectId(n.getProject().getId())
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }


    public static NotificationPayload system(String msg) {
        return NotificationPayload.builder()
                .type("SYSTEM")
                .message(msg)
                .read(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

}
