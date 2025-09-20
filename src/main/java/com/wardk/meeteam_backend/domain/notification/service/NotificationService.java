package com.wardk.meeteam_backend.domain.notification.service;


import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.notification.repository.NotificationRepository;
import com.wardk.meeteam_backend.web.notification.dto.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Slice<NotificationResponse> getNotifications(Long memberId, Pageable pageable) {

        Slice<NotificationResponse> response = notificationRepository.findByMemberId
                (memberId, pageable).
                map(notification -> new NotificationResponse(notification));

        return response;

    }


}
