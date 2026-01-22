package com.wardk.meeteam_backend.web.notification.context;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.notification.NotificationEvent;
import com.wardk.meeteam_backend.domain.notification.entity.Notification;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
public class NotificationContext {

    Long receiverId;
    Long projectId;
    Long actorId;
    Long applicationId;
    LocalDate occuredAt;

    @Builder
    private NotificationContext(Long receiverId, Long projectId, Long actorId, Long applicationId, LocalDate occuredAt) {
        this.receiverId = receiverId;
        this.projectId = projectId;
        this.actorId = actorId;
        this.applicationId = applicationId;
        this.occuredAt = occuredAt;
    }

    public static NotificationContext from(NotificationEvent event) {
        return NotificationContext.builder()
                .receiverId(event.getReceiverId())
                .projectId(event.getProjectId())
                .actorId(event.getActorId())
                .occuredAt(LocalDate.now())
                .build();
    }

}
