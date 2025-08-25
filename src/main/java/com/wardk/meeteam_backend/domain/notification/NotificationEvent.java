package com.wardk.meeteam_backend.domain.notification;


import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data
// 알림에 필요한 최소 정보만 담는 POJO
public class NotificationEvent {

    private Long receiverId;          // 알림 받을 사람
    private Long projectId;           // 관련 프로젝트
    private Long actorId;            // 행위자(지원자) - 없으면 null
    private NotificationType type;      // 예: PROJECT_APPLY

    // 메시지
}
