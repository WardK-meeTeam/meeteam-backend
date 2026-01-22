package com.wardk.meeteam_backend.web.notification.payload;

import com.wardk.meeteam_backend.web.notification.ApprovalResult;
import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
public class ProjectApplicationApprovedPayload implements Payload{


    private Long receiverId; // 알림 받는 사람 id
    private Long projectId;
    private ApprovalResult approvalResult;
    private LocalDate date;

    public static Payload create(NotificationContext context) {
        return ProjectApplicationApprovedPayload.builder()
                .receiverId(context.getReceiver().getId())
                .projectId(context.getProject().getId())
                .approvalResult(ApprovalResult.APPROVED)
                .build();
    }
}
