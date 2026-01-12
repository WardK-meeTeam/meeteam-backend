package com.wardk.meeteam_backend.web.notification.payload;

import com.wardk.meeteam_backend.web.notification.context.NotificationContext;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@Builder
@RequiredArgsConstructor
public class ApplySelfApplyPayloadStrategy implements Payload {

    /**
     * 프로젝트 지원자가 지원을 완료하면 받는 본문내용
     */

    private Long receiverId;

    private String projectName;

    private LocalDate localDate;


    public static Payload create(NotificationContext context) {
        return ApplySelfApplyPayloadStrategy.builder()
                .receiverId(context.getReceiver().getId())
                .projectName(context.getProject().getName())
                .localDate(LocalDate.now())
                .build();
    }

}
