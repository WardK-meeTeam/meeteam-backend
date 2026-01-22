package com.wardk.meeteam_backend.web.notification.payload;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.time.LocalDate;


@AllArgsConstructor
@Data
@Builder
public class ProjectApplicationReceivedPayload implements Payload {

    private Long applicationId; // 지원서Id
    private Long projectId;
    private Long receiverId;
    private Long applicantId;
    private String applicantName;
    private String projectName;
    private LocalDate date;


    public static Payload create(NotificationContext context, Member applicant) {
        return ProjectApplicationReceivedPayload.builder()
                .applicationId(context.getApplicationId())
                .projectId(context.getProject().getId())
                .receiverId(context.getReceiver().getId())
                .applicantId(context.getActorId())
                .applicantName(applicant.getRealName())
                .projectName(context.getProject().getName())
                .date(LocalDate.now())
                .build();
    }
}
