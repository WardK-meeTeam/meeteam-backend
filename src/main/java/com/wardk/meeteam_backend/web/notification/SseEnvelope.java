package com.wardk.meeteam_backend.web.notification;

import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
public class SseEnvelope<T> {

    private final NotificationType type;
    private final T data;
    private final LocalDate createdAt;
}
