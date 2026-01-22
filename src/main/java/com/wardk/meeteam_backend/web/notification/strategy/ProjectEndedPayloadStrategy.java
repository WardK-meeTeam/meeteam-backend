package com.wardk.meeteam_backend.web.notification.strategy;

import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import com.wardk.meeteam_backend.web.notification.payload.Payload;

public class ProjectEndedPayloadStrategy implements NotificationPayloadStrategy{
    @Override
    public NotificationType getType() {
        return null;
    }

    @Override
    public Payload create(NotificationContext context) {
        return null;
    }
}
