package com.wardk.meeteam_backend.web.notification.payload;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class ProjectEndNotiPayload implements Payload {


    private Long projectId;
    private Long memberId;
    //
    private String projectName;
    private LocalDate occurredAt;
}
