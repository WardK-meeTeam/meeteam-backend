package com.wardk.meeteam_backend.web.notification.factory;


import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import com.wardk.meeteam_backend.web.notification.payload.Payload;
import com.wardk.meeteam_backend.web.notification.strategy.NotificationPayloadStrategy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class NotificationPayloadFactory {


    private final Map<NotificationType, NotificationPayloadStrategy> strategies;


    public NotificationPayloadFactory(List<NotificationPayloadStrategy> list) {

        this.strategies = list.stream()
                .collect(Collectors.toMap(
                        st -> st.getType(),
                        st -> st
                ));
    }


    public Payload create(NotificationEvent event) {
        NotificationPayloadStrategy strategy =
                strategies.get(event.getType());

        if (strategy == null) {
            throw new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        return strategy.create(
                NotificationContext.from(event)
        );
    }



}
