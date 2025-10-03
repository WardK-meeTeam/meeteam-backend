package com.wardk.meeteam_backend.web.notification.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ApplyNotiPayload implements Payload {

    /**
     * 프로젝트 지원자가 지원을 완료하면 받는 본문내용
     */

    private Long receiverId;

    private String projectName;

    private LocalDate localDate;

}
